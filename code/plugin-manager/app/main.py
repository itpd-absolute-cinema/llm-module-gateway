"""Plugin Manager: прогоняет chat-запрос / ответ LLM через цепочку плагинов."""
import logging
import os
from copy import deepcopy
from typing import Callable

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from plugin_api import Forbidden, PluginReject

from .loader import load_plugin
from .schemas import (ChatCompletionResponse, ChatRequest, PluginSpec,
                      RequestFiltersInput, ResponseFiltersInput)

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"),
                    format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("plugin-manager")

app = FastAPI(title="Plugin Manager", version="0.2.0")


def error(status: int, message: str, type_: str, code: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={
        "error": {"message": message, "type": type_, "param": None, "code": code}})


def run_chain(kind: str, data: dict, plugins: list[PluginSpec], model_cls,
              call: Callable[[object, dict], dict]) -> dict:
    """Прогоняет data по плагинам по порядку.

    - PluginReject (InvalidData/Forbidden) прерывает цепочку и уходит наверх (400/403);
    - любой другой сбой (нет файла, ошибка в коде, невалидный результат) -> плагин пропускается.
    """
    log.info("%s: running %d plugin(s): %s", kind, len(plugins), [p.name for p in plugins])
    for spec in plugins:
        if spec.type != kind:
            log.warning("plugin %s has type %s, expected %s: skipped", spec.name, spec.type, kind)
            continue
        try:
            plugin = load_plugin(spec.name, kind, spec.options)
            result = call(plugin, deepcopy(data))  # плагин работает с копией
            new = model_cls.model_validate(result).model_dump(exclude_unset=True)
            log.info("plugin %s applied: %s", spec.name, "changed" if new != data else "no changes")
            data = new
        except PluginReject as exc:
            exc.plugin = spec.name
            raise
        except Exception:
            log.exception("plugin %s failed, skipped", spec.name)
    return data


@app.post("/request-filters", response_model=None)
def run_request_filters(body: RequestFiltersInput):
    return run_chain("request_filter", body.request.model_dump(exclude_unset=True),
                     body.plugins, ChatRequest, lambda p, d: p.apply(d))


@app.post("/response-filters", response_model=None)
def run_response_filters(body: ResponseFiltersInput):
    req = body.request.model_dump(exclude_unset=True)
    return run_chain("response_filter", body.response.model_dump(exclude_unset=True),
                     body.plugins, ChatCompletionResponse,
                     lambda p, d: p.apply(d, deepcopy(req)))


@app.get("/health", include_in_schema=False)
def health():
    return {"status": "ok"}


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