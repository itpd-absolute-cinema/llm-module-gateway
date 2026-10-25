import asyncio
import logging
from contextlib import asynccontextmanager
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.openapi.utils import get_openapi
from fastapi.responses import JSONResponse
from openai import APIConnectionError, APIStatusError, APITimeoutError, AsyncOpenAI
from starlette.exceptions import HTTPException

from gateway.admin import PluginManager, router as admin_router
from gateway.config import config_path, factories, load, secret
from gateway.core import Context, Response, compose, error
from gateway.schema import ChatRequest, error_response

logger = logging.getLogger("gateway")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s %(message)s")
    # HTTP client debug output can include request payloads and credentials.
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("openai").setLevel(logging.WARNING)
    path = config_path()
    config = load(path)
    async with AsyncOpenAI(
        base_url=str(config.upstream.base_url),
        api_key=secret(config.upstream.api_key_env),
        timeout=config.upstream.timeout_seconds,
        max_retries=0,
    ) as client:
        async def upstream(context: Context) -> Response:
            try:
                async with asyncio.timeout(config.upstream.timeout_seconds):
                    completion = await client.chat.completions.create(**context.body)
                return Response(completion.model_dump(mode="json", exclude_none=True))
            except (TimeoutError, APITimeoutError):
                return error(504, "Upstream request timed out.", "upstream_timeout")
            except APIConnectionError:
                return error(502, "Could not connect to upstream.", "upstream_unavailable")
            except APIStatusError as exc:
                logger.warning(
                    "upstream_error request_id=%s upstream_status=%s",
                    context.request_id, exc.status_code,
                )
                status = 429 if exc.status_code == 429 else 502
                return error(status, "Upstream rejected the request.", "upstream_error")

        app.state.handler = compose(upstream, factories(config))
        app.state.plugin_manager = PluginManager(config, path)
        yield


app = FastAPI(title="LLM Gateway Prototype", lifespan=lifespan)
app.include_router(admin_router)


def openapi_schema():
    if app.openapi_schema is None:
        schema = get_openapi(title=app.title, version=app.version, routes=app.routes)
        # Validation failures use our 400 envelope, not FastAPI's default 422.
        # Keep explicitly declared 422 responses for non-TOML plugin options.
        for path in schema["paths"].values():
            for operation in path.values():
                responses = operation.get("responses", {})
                validation = responses.get("422", {}).get("content", {}).get("application/json", {}).get("schema", {})
                if validation.get("$ref") == "#/components/schemas/HTTPValidationError":
                    del responses["422"]
        for name in ("HTTPValidationError", "ValidationError"):
            schema.get("components", {}).get("schemas", {}).pop(name, None)
        app.openapi_schema = schema
    return app.openapi_schema


app.openapi = openapi_schema


def http_response(result: Response, request_id: str) -> JSONResponse:
    headers = {"x-request-id": request_id}
    if result.status == 401:
        headers["www-authenticate"] = "Bearer"
    return JSONResponse(result.body, status_code=result.status, headers=headers)


@app.exception_handler(RequestValidationError)
async def invalid_request(request: Request, exc: RequestValidationError):
    # Validation errors contain submitted values; never echo or log them.
    if request.url.path.startswith("/admin/"):
        return http_response(error(400, "Invalid admin request. Check the documented fields.", "invalid_request"), uuid4().hex)
    return http_response(
        error(400, "Invalid request. Use the documented non-streaming text chat fields.", "invalid_request"),
        uuid4().hex,
    )


@app.exception_handler(HTTPException)
async def http_error(request: Request, exc: HTTPException):
    message = exc.detail if request.url.path.startswith("/admin/") else "HTTP request rejected."
    return http_response(error(exc.status_code, message, "http_error"), uuid4().hex)


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/v1/chat/completions", responses={
    400: error_response("Malformed JSON or invalid non-streaming text chat fields."),
    401: error_response("Missing or invalid bearer credentials when authentication is enabled.", unauthorized=True),
    403: error_response("The requested model is blocked by the model allowlist plugin."),
    429: error_response("The upstream provider rejected the request with a rate-limit response."),
    500: error_response("Unexpected gateway or plugin processing failure."),
    502: error_response("Could not connect to upstream, or upstream returned a non-429 error response."),
    504: error_response("The upstream request timed out."),
})
async def chat(body: ChatRequest, request: Request):
    context = Context(
        body=body.model_dump(exclude_none=True),
        request_id=uuid4().hex,
        authorization=request.headers.get("authorization"),
    )
    try:
        result = await request.app.state.handler(context)
    except Exception as exc:
        logger.error("request_failed request_id=%s exception=%s", context.request_id, type(exc).__name__)
        result = error(500, "Gateway processing failed.", "gateway_error")
    return http_response(result, context.request_id)
