"""Plugin Manager: прогоняет chat-запрос / ответ LLM через цепочку плагинов."""
import logging
import math
import os
from contextlib import asynccontextmanager
from pathlib import Path
from time import perf_counter

from fastapi import FastAPI, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from prometheus_client import CONTENT_TYPE_LATEST, REGISTRY, generate_latest

from plugin_api import Forbidden, InvalidData, PluginReject

from . import loader, metrics
from .sandbox import PluginBusy, PluginCrashed, PluginTimeout, Sandbox
from .schemas import (ChatCompletionResponse, ChatRequest, PluginSpec,
                      RequestFiltersInput, ResponseFiltersInput)

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
logging.basicConfig(level=LOG_LEVEL, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("plugin-manager")


def _env_float(name: str, default: float) -> float:
    try:
        value = float(os.getenv(name, default))
        if math.isfinite(value) and value > 0:
            return value
    except ValueError:
        pass
    log.warning("%s: invalid value, using %s", name, default)
    return default


DEFAULT_TIMEOUT = _env_float("PLUGIN_TIMEOUT_SECONDS", 5.0)
QUEUE_TIMEOUT = _env_float("PLUGIN_QUEUE_TIMEOUT_SECONDS", 5.0)
MAX_TIMEOUT = 600.0
TIMEOUT_OPTION = "timeout_seconds"  # зарезервированный ключ options: свой лимит времени для плагина

sandbox = Sandbox(size=int(_env_float("PLUGIN_WORKERS", 4)), log_level=LOG_LEVEL)


@asynccontextmanager
async def lifespan(_: FastAPI):
    sandbox.start()
    yield
    sandbox.close()


app = FastAPI(title="Plugin Manager", version="0.3.0", lifespan=lifespan)


def error(status: int, message: str, type_: str, code: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={
        "error": {"message": message, "type": type_, "param": None, "code": code}})


def split_options(spec: PluginSpec) -> tuple[dict, float]:
    """(options для плагина без зарезервированного ключа, лимит времени в секундах)."""
    options = dict(spec.options)
    if TIMEOUT_OPTION not in options:
        return options, DEFAULT_TIMEOUT
    raw = options.pop(TIMEOUT_OPTION)
    if isinstance(raw, (int, float)) and not isinstance(raw, bool) and math.isfinite(raw) and 0 < raw <= MAX_TIMEOUT:
        return options, float(raw)
    log.warning("plugin %s: option %s=%r is invalid (need a number in (0, %d]), using the default %.1fs",
                spec.name, TIMEOUT_OPTION, raw, MAX_TIMEOUT, DEFAULT_TIMEOUT)
    return options, DEFAULT_TIMEOUT


def run_chain(kind: str, data: dict, plugins: list[PluginSpec], model_cls, request: dict | None = None) -> dict:
    """Прогоняет data по плагинам по порядку.

    - PluginReject (InvalidData/Forbidden) прерывает цепочку и уходит наверх (400/403);
    - любой другой сбой (нет файла, ошибка в коде, невалидный результат, таймаут, нет свободного
      процесса) -> плагин пропускается, данные идут дальше без изменений.
    Каждый плагин выполняется в отдельном процессе песочницы и получает свою копию данных.
    """
    log.info("%s: running %d plugin(s): %s", kind, len(plugins), [p.name for p in plugins])
    for spec in plugins:
        if spec.type != kind:
            log.warning("plugin %s has type %s, expected %s: skipped", spec.name, spec.type, kind)
            continue
        started = perf_counter()
        outcome = "error"
        try:
            options, timeout = split_options(spec)
            declared = metrics.registry.declare(spec.name, loader.declared_metrics(spec.name))
            args = (data,) if kind == "request_filter" else (data, request)
            try:
                reply = sandbox.run(spec.name, kind, options, args, timeout=timeout,
                                    queue_timeout=QUEUE_TIMEOUT, plugins_dir=loader.PLUGINS_DIR)
            except PluginTimeout:
                outcome = "timeout"
                metrics.RESTARTS.labels(reason="timeout").inc()
                log.error("plugin %s timed out after %.1fs, skipped (its process was killed)", spec.name, timeout)
                continue
            except PluginCrashed:
                metrics.RESTARTS.labels(reason="crash").inc()
                log.error("plugin %s crashed its worker process, skipped", spec.name)
                continue
            except PluginBusy:
                outcome = "busy"
                log.warning("plugin %s skipped: no free sandbox worker within %.1fs", spec.name, QUEUE_TIMEOUT)
                continue

            status = reply[0]
            metrics.registry.apply(spec.name, declared, reply[-1])
            if status == "reject":
                outcome = "rejected"
                exc = (Forbidden if reply[1] else InvalidData)(reply[2])
                exc.plugin = spec.name
                raise exc
            if status == "error":
                log.error("plugin %s failed, skipped\n%s", spec.name, reply[1].rstrip())
                continue
            try:
                new = model_cls.model_validate(reply[1]).model_dump(exclude_unset=True)
            except Exception:
                log.exception("plugin %s failed, skipped: invalid result", spec.name)
                continue
            outcome = "ok"
            log.info("plugin %s applied: %s", spec.name, "changed" if new != data else "no changes")
            data = new
        except PluginReject:
            raise
        except Exception:
            log.exception("plugin %s failed, skipped", spec.name)
        finally:
            elapsed = perf_counter() - started
            metrics.observe_call(spec.name, kind, outcome, elapsed)
            log.info("plugin %s stage=%s duration_ms=%.3f outcome=%s", spec.name, kind, elapsed * 1000, outcome)
    return data


@app.post("/request-filters", response_model=None)
def run_request_filters(body: RequestFiltersInput):
    return run_chain("request_filter", body.request.model_dump(exclude_unset=True),
                     body.plugins, ChatRequest)


@app.post("/response-filters", response_model=None)
def run_response_filters(body: ResponseFiltersInput):
    return run_chain("response_filter", body.response.model_dump(exclude_unset=True),
                     body.plugins, ChatCompletionResponse,
                     request=body.request.model_dump(exclude_unset=True))


@app.get("/health", include_in_schema=False)
def health():
    return {"status": "ok"}


@app.get("/metrics", include_in_schema=False)
def prometheus_metrics():
    return Response(generate_latest(REGISTRY), media_type=CONTENT_TYPE_LATEST)


@app.exception_handler(PluginReject)
async def on_reject(_: Request, exc: PluginReject):
    forbidden = isinstance(exc, Forbidden)
    log.info("plugin %s rejected: %s", getattr(exc, "plugin", "?"), exc)
    return error(403 if forbidden else 400, f"{getattr(exc, 'plugin', 'plugin')}: {exc}",
                 "forbidden_error" if forbidden else "invalid_request_error",
                 "plugin_forbidden" if forbidden else "plugin_invalid_data")


@app.exception_handler(RequestValidationError)
async def on_validation(_: Request, exc: RequestValidationError):
    return error(422, str(exc.errors()), "invalid_request_error", "invalid_input")


@app.exception_handler(Exception)
async def on_unexpected(_: Request, exc: Exception):
    log.exception("unexpected error")
    return error(500, "internal error", "server_error", "internal_error")
