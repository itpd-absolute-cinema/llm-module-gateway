import asyncio
import copy
import hmac
import json
import keyword
import logging
import os
import re
import tempfile
from pathlib import Path
from typing import Annotated

import tomlkit
from fastapi import (
    APIRouter,
    Depends,
    File,
    Form,
    HTTPException,
    Request,
    Response,
    UploadFile,
)
from fastapi.routing import APIRoute
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, ConfigDict, Field, model_validator

from gateway.config import Config, PluginConfig
from gateway.schema import error_response

PLUGIN_DIRECTORY = Path(__file__).parent / "plugins"
MAX_PLUGIN_BYTES = 1024 * 1024
logger = logging.getLogger("gateway.admin")
bearer = HTTPBearer(auto_error=False)


async def authorize_admin(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
):
    key = os.environ.get("GATEWAY_ADMIN_API_KEY")
    if not key:
        raise HTTPException(503, "Plugin administration is not configured.")
    if credentials is None or not hmac.compare_digest(
        credentials.credentials.encode("utf-8"), key.encode("utf-8")
    ):
        raise HTTPException(401, "Invalid admin credentials.")


class AdminRoute(APIRoute):
    def get_route_handler(self):
        handler = super().get_route_handler()

        async def handle(request: Request):
            try:
                response = await handler(request)
                manager = request.app.state.plugin_manager
                response.headers["X-Restart-Required"] = str(manager.restart_required).lower()
                return response
            except OSError as exc:
                logger.error("plugin_storage_failed exception=%s", type(exc).__name__)
                raise HTTPException(500, "Could not update plugin storage.") from None

        return handle


router = APIRouter(
    prefix="/admin/plugins", tags=["Plugin administration"],
    dependencies=[Depends(authorize_admin)], route_class=AdminRoute,
    responses={
        400: error_response("Malformed request, invalid plugin name, or symlink plugin."),
        401: error_response("Missing or invalid admin bearer credentials.", unauthorized=True),
        409: error_response("Multiple configuration entries exist for a plugin."),
        500: error_response("Plugin storage operation failed."),
        503: error_response("Plugin administration is disabled because GATEWAY_ADMIN_API_KEY is unset."),
    },
)

RESTART_RESPONSE = {
    "headers": {
        "X-Restart-Required": {
            "description": "Whether saved enabled plugins differ from the running pipeline.",
            "schema": {"type": "string", "enum": ["true", "false"]},
        },
    },
}
NOT_FOUND_RESPONSE = error_response("Plugin source and configuration entry were not found.")
CONFIG_CONFLICT_RESPONSE = error_response(
    "Multiple configuration entries exist, or configuration changed on disk; restart before editing."
)
OPTIONS_RESPONSE = error_response("Options cannot be represented in TOML, for example JSON null.")


class Settings(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    options: dict
    before: str | None = Field(default=None, description="Move immediately before this configured plugin; mutually exclusive with after.")
    after: str | None = Field(default=None, description="Move immediately after this configured plugin; mutually exclusive with before.")


class Status(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    enabled: bool | None = None
    before: str | None = Field(default=None, description="Move immediately before this configured plugin; mutually exclusive with after.")
    after: str | None = Field(default=None, description="Move immediately after this configured plugin; mutually exclusive with before.")

    @model_validator(mode="after")
    def validate_change(self):
        if "enabled" in self.model_fields_set and self.enabled is None:
            raise ValueError("enabled must be a boolean.")
        if self.enabled is None and self.before is None and self.after is None:
            raise ValueError("Provide enabled, before, or after.")
        return self


class PluginInfo(BaseModel):
    name: str
    enabled: bool
    options: dict


class RunningPluginInfo(PluginInfo):
    enabled: bool = Field(description="Whether this process loaded the plugin at startup.")
    options: dict = Field(description="Options used by the running plugin; empty when inactive.")
    restart_required: bool = Field(description="Whether saved enabled state, options or enabled-chain position differ from this process.")


def valid_name(name: str) -> bool:
    return bool(re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", name)) and not (
        name.startswith("__") or keyword.iskeyword(name)
    )


def validate_options(options: dict):
    try:
        tomlkit.dumps({"options": options})
    except (TypeError, ValueError):
        raise HTTPException(422, "Options must be representable in TOML; null is not supported.") from None


class PluginManager:
    def __init__(self, config: Config, path: Path):
        self.config = config
        self.path = path
        self.startup_plugins = [item.model_copy(deep=True) for item in config.plugins if item.enabled]
        self.directory = PLUGIN_DIRECTORY
        self.lock = asyncio.Lock()
        self.source = path.read_text(encoding="utf-8")

    @property
    def restart_required(self) -> bool:
        configured = [item.model_dump_json() for item in self.config.plugins if item.enabled]
        running = [item.model_dump_json() for item in self.startup_plugins]
        return configured != running

    def plugin_path(self, name: str) -> Path:
        if not valid_name(name):
            raise HTTPException(400, "Use a Python module name without a .py suffix.")
        path = self.directory / (name + ".py")
        if path.is_symlink():
            raise HTTPException(400, "Symlink plugins cannot be managed.")
        return path

    def entry(self, name: str) -> PluginConfig | None:
        module = f"gateway.plugins.{name}:"
        entries = [item for item in self.config.plugins if item.factory.startswith(module)]
        if len(entries) > 1:
            raise HTTPException(409, "Multiple configuration entries exist for this plugin.")
        return entries[0] if entries else None

    def info(self, name: str) -> PluginInfo:
        path = self.plugin_path(name)
        item = self.entry(name)
        if not path.is_file() and item is None:
            raise HTTPException(404, "Plugin not found.")
        return PluginInfo(name=name, enabled=item.enabled if item else False, options=item.options if item else {})

    def running_info(self, name: str) -> RunningPluginInfo:
        path = self.plugin_path(name)
        saved = self.entry(name)
        module = f"gateway.plugins.{name}:"
        running = next((item for item in self.startup_plugins if item.factory.startswith(module)), None)
        if not path.is_file() and saved is None and running is None:
            raise HTTPException(404, "Plugin not found.")
        configured = [item for item in self.config.plugins if item.enabled]
        desired = saved if saved is not None and saved.enabled else None
        changed = (running.model_dump_json() if running else None) != (desired.model_dump_json() if desired else None)
        if running is not None and desired is not None:
            changed = changed or self.startup_plugins.index(running) != configured.index(desired)
        return RunningPluginInfo(
            name=name, enabled=running is not None, options=running.options if running else {},
            restart_required=changed,
        )

    def listing(self) -> list[RunningPluginInfo]:
        names = {path.stem for path in self.directory.glob("*.py") if valid_name(path.stem) and not path.is_symlink()}
        for item in [*self.config.plugins, *self.startup_plugins]:
            module = item.factory.split(":", 1)[0]
            if module.startswith("gateway.plugins."):
                name = module.removeprefix("gateway.plugins.")
                if valid_name(name):
                    names.add(name)
        return [self.running_info(name) for name in sorted(names)]

    def save(self, config: Config):
        # Do not silently overwrite edits made outside this process.
        if self.path.read_text(encoding="utf-8") != self.source:
            raise HTTPException(409, "Configuration changed on disk; restart before editing plugins.")
        if config.model_dump_json() == self.config.model_dump_json():
            return
        document = tomlkit.parse(self.source)
        tables = tomlkit.aot()
        old_tables = document.get("plugins", [])
        for item in config.plugins:
            old = next((table for table in old_tables if table.get("factory") == item.factory), None)
            table = copy.deepcopy(old) if old is not None else tomlkit.table()
            table["factory"] = item.factory
            table["enabled"] = item.enabled
            old_options = tomlkit.dumps({"options": table.unwrap().get("options", {})})
            if old_options != tomlkit.dumps({"options": item.options}):
                table["options"] = item.options
            tables.append(table)
        document["plugins"] = tables if config.plugins else []
        source = tomlkit.dumps(document)

        temporary = None
        try:
            with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=self.path.parent, delete=False) as file:
                temporary = Path(file.name)
                file.write(source)
                file.flush()
                os.fsync(file.fileno())
            temporary.chmod(self.path.stat().st_mode & 0o777)
            temporary.replace(self.path)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)
        self.source = source
        self.config = config

    def update(
        self, name: str, *, options: dict | None = None, enabled: bool | None = None,
        before: str | None = None, after: str | None = None,
    ):
        self.info(name)
        if before is not None and after is not None:
            raise HTTPException(400, "Provide only one of before or after.")
        target_name = before if before is not None else after
        target = None
        if target_name is not None:
            self.plugin_path(target_name)
            if target_name == name:
                raise HTTPException(400, "A plugin cannot be placed relative to itself.")
            target = self.entry(target_name)
            if target is None:
                raise HTTPException(404, "Ordering target has no configuration entry.")
        config = self.config.model_copy(deep=True)
        existing = self.entry(name)
        if existing is None:
            item = PluginConfig(factory=f"gateway.plugins.{name}:factory", enabled=False)
            config.plugins.append(item)
        else:
            item = config.plugins[self.config.plugins.index(existing)]
        if options is not None:
            validate_options(options)
            item.options = options
        if enabled is not None:
            item.enabled = enabled
        if target is not None:
            config.plugins.remove(item)
            index = config.plugins.index(target)
            config.plugins.insert(index + (after is not None), item)
        self.save(config)
        return self.info(name)

    def clear_bytecode(self, name: str):
        for path in (self.directory / "__pycache__").glob(f"{name}.*.pyc"):
            path.unlink(missing_ok=True)


@router.get("", response_model=list[RunningPluginInfo], responses={200: RESTART_RESPONSE})
async def list_plugins(request: Request):
    return request.app.state.plugin_manager.listing()


@router.get("/{name}", response_model=RunningPluginInfo, responses={
    200: RESTART_RESPONSE,
    404: NOT_FOUND_RESPONSE,
})
async def get_plugin(name: str, request: Request):
    return request.app.state.plugin_manager.running_info(name)


@router.post("", status_code=201, response_model=PluginInfo, responses={
    201: RESTART_RESPONSE,
    400: error_response("Malformed multipart request, invalid filename or Python source, symlink plugin, invalid ordering hint, or options that are not a JSON object."),
    404: error_response("Ordering target has no configuration entry."),
    409: error_response("Plugin already exists, multiple configuration entries exist, or configuration changed on disk."),
    413: error_response("Plugin source exceeds the 1 MiB limit."),
    422: OPTIONS_RESPONSE,
})
async def upload_plugin(
    request: Request, file: Annotated[UploadFile, File()], options: Annotated[str, Form()] = "{}",
    before: Annotated[str | None, Form(description="Insert immediately before this configured plugin; mutually exclusive with after.")] = None,
    after: Annotated[str | None, Form(description="Insert immediately after this configured plugin; mutually exclusive with before.")] = None,
):
    manager = request.app.state.plugin_manager
    filename = file.filename or ""
    if not filename.endswith(".py"):
        raise HTTPException(400, "Upload a .py file.")
    name = filename[:-3]
    path = manager.plugin_path(name)
    try:
        parsed = json.loads(options)
    except ValueError:
        raise HTTPException(400, "Options must be a JSON object.") from None
    if not isinstance(parsed, dict):
        raise HTTPException(400, "Options must be a JSON object.")
    validate_options(parsed)
    source = await file.read(MAX_PLUGIN_BYTES + 1)
    if len(source) > MAX_PLUGIN_BYTES:
        raise HTTPException(413, "Plugin files must not exceed 1 MiB.")
    try:
        compile(source, filename, "exec")
    except (SyntaxError, ValueError):
        raise HTTPException(400, "Plugin contains invalid Python source.") from None
    async with manager.lock:
        if path.exists() or manager.entry(name) is not None:
            raise HTTPException(409, "Plugin already exists.")
        manager.clear_bytecode(name)
        try:
            output = path.open("xb")
        except FileExistsError:
            raise HTTPException(409, "Plugin already exists.") from None
        try:
            with output:
                output.write(source)
            return manager.update(name, options=parsed, before=before, after=after)
        except Exception:
            path.unlink(missing_ok=True)
            raise


@router.put("/{name}", response_model=PluginInfo, responses={
    200: RESTART_RESPONSE,
    400: error_response("Malformed request, invalid plugin name, symlink plugin, or invalid ordering hint."),
    404: error_response("Plugin not found, or ordering target has no configuration entry."),
    409: CONFIG_CONFLICT_RESPONSE,
    422: OPTIONS_RESPONSE,
})
async def update_settings(name: str, settings: Settings, request: Request):
    manager = request.app.state.plugin_manager
    async with manager.lock:
        return manager.update(name, options=settings.options, before=settings.before, after=settings.after)


@router.patch("/{name}", response_model=PluginInfo, responses={
    200: RESTART_RESPONSE,
    400: error_response("Malformed request, invalid plugin name, symlink plugin, or invalid ordering hint."),
    404: error_response("Plugin not found, or ordering target has no configuration entry."),
    409: CONFIG_CONFLICT_RESPONSE,
})
async def set_status(name: str, status: Status, request: Request):
    manager = request.app.state.plugin_manager
    async with manager.lock:
        return manager.update(name, enabled=status.enabled, before=status.before, after=status.after)


@router.delete("/{name}", status_code=204, responses={
    204: RESTART_RESPONSE,
    404: NOT_FOUND_RESPONSE,
    409: error_response("Plugin is enabled or still running, multiple configuration entries exist, or configuration changed on disk."),
})
async def delete_plugin(name: str, request: Request):
    manager = request.app.state.plugin_manager
    async with manager.lock:
        if manager.info(name).enabled:
            raise HTTPException(409, "Disable the plugin before deleting it.")
        if any(item.factory.startswith(f"gateway.plugins.{name}:") for item in manager.startup_plugins):
            raise HTTPException(409, "Plugin is still active; restart before deleting it.")
        path = manager.plugin_path(name)
        config = manager.config.model_copy(deep=True)
        config.plugins = [item for item in config.plugins if item.factory != getattr(manager.entry(name), "factory", None)]
        # Stage removal so a failed config write can restore the source file.
        with tempfile.TemporaryDirectory(dir=manager.directory) as directory:
            staged = Path(directory) / path.name
            if path.exists():
                path.replace(staged)
            try:
                manager.clear_bytecode(name)
                manager.save(config)
            except Exception:
                if staged.exists():
                    staged.replace(path)
                raise
    return Response(status_code=204)
