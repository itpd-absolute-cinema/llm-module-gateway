# Python gateway prototype

A standalone experiment with Django-style async middleware.
It does not replace the project's Java/Python architecture decision.
No automated tests are included; verification uses the running HTTP servers.

## Run locally

Install [uv](https://docs.astral.sh/uv/) and run these commands from `prototype/`.
`uv` installs the pinned Python version if needed.

```sh
uv sync --locked
```

In one terminal, start the demonstration provider:

```sh
uv run uvicorn demo_upstream:app --host 127.0.0.1 --port 9001 --no-access-log
```

In another terminal, supply local demonstration credentials and start the gateway:

```sh
export GATEWAY_API_KEY=local-demo-key
export UPSTREAM_API_KEY=local-demo-provider-key
uv run uvicorn gateway.app:app --host 127.0.0.1 --port 8000 --no-access-log
```

These credentials are examples for localhost only.
The demo provider makes no model calls and ignores its upstream credential.
It returns fixed text and reports zero usage rather than estimating tokens.

Call the gateway with an ordinary OpenAI client:

```sh
uv run python - <<'PY'
from openai import OpenAI

client = OpenAI(base_url="http://127.0.0.1:8000/v1", api_key="local-demo-key")
response = client.chat.completions.create(
    model="demo-model",
    messages=[{"role": "user", "content": "Please mask SECRET."}],
)
print(response.choices[0].message.content)
PY
```

Expected output: `Demo response containing [REDACTED].`
The logging plugin records a request ID, status 200, and elapsed milliseconds.
The HTTP response includes the same ID in `x-request-id`.
`GET /health` reports process readiness, without making an upstream request.

## Supported HTTP contract

`POST /v1/chat/completions` accepts OpenAI-shaped JSON and bearer authentication.
The supported subset is:

- Required `model` and nonempty `messages`.
- String message content with roles `system`, `developer`, `user`, or `assistant`.
- Optional `temperature`, `top_p`, `stop`, and either `max_tokens` or `max_completion_tokens`.
- Omitted `stream` or `stream: false`.

Streaming, tools, multimodal content, other fields, and other OpenAI endpoints are not implemented.
Invalid or unsupported bodies return HTTP 400 with an `error` object containing `message`, `type`, `param`, and `code`.
FastAPI validates input before invoking plugins, so malformed requests do not reach authentication or the logging plugin.

Missing or invalid gateway credentials return 401; a disallowed model returns 403.
Neither rejection calls the upstream.
Upstream timeouts return 504, connectivity failures return 502, and upstream rate limits return 429.
Other upstream HTTP errors return a generic 502 without forwarding the provider's error text.
Unexpected plugin exceptions stop processing and return a generic 500.
Retries are disabled to avoid duplicate provider calls.

## Configuration and ordering

Edit `config.toml`, or set `GATEWAY_CONFIG` to another TOML file path.
Relative paths resolve from the working directory.
Configuration and plugins load once at startup; restart after changes.
Secrets come from the environment variables named in configuration, not TOML.
Environment files are not loaded automatically.

The sample enables five plugins, outermost first:

| Plugin | Behavior |
| :--- | :--- |
| `request_logging` | Logs duration, outcome, and request ID, including plugin exceptions |
| `authentication` | Checks a gateway bearer key using a constant-time comparison |
| `model_allowlist` | Rejects models outside the configured list |
| `literal_redaction` | Replaces configured literals in request and successful response message text |
| `token_cap` | Supplies a default output-token limit or clamps it to the maximum |

Requests enter in listed order; responses return in reverse order.
A plugin can return a `Response` without calling the next handler.
Inner plugins and the provider then do not run.
Exceptions propagate through the chain to the HTTP boundary, unlike Django's per-layer exception conversion.

The token plugin preserves whichever token-limit field the client supplied.
Its `parameter` option selects the field used when the client supplies neither.
Configure `max_completion_tokens` for providers/models that require it.
This limits the requested generation budget; it does not count prompt tokens or enforce provider billing.

To call a real provider, change `upstream.base_url`, update the model allowlist, and set `UPSTREAM_API_KEY` to its credential.
Requests then leave your machine and may incur provider charges.
The inbound gateway key is never used as the upstream key.

## Add a plugin

Create an importable Python module, for example `gateway/plugins/prefix.py`:

```python
from gateway.core import Context, Handler, Response


def factory(next_handler: Handler, *, prefix: str = "") -> Handler:
    async def handle(context: Context) -> Response:
        response = await next_handler(context)
        if response.status == 200:
            for choice in response.body.get("choices", []):
                message = choice["message"]
                if isinstance(message.get("content"), str):
                    message["content"] = prefix + message["content"]
        return response

    return handle
```

Add a configuration entry and restart:

```toml
[[plugins]]
factory = "gateway.plugins.prefix:factory"
[plugins.options]
prefix = "Company assistant: "
```

No executor or HTTP-adapter changes are needed.
The factory runs once per worker; its handler runs per request.
Put request state in `Context` or local variables, not shared factory state.
Call `next_handler` once to continue, or return without calling it to stop.
The runtime does not prevent a trusted plugin from calling it multiple times.

## Live inspection and limits

The demo provider accepts `DEMO_DELAY_SECONDS` and `DEMO_STATUS_CODE` in its environment.
For example, starting it with `DEMO_DELAY_SECONDS=3` exceeds the sample gateway's two-second upstream deadline.
A status of 503 demonstrates the gateway's generic 502 response.
These controls belong only to the local demo, not to the gateway's request API.

Gateway logs omit bodies, credentials, and exception messages.
Provider debug logs and Uvicorn access logs are disabled in the startup commands.
The demo and gateway should stay bound to localhost during this experiment.
Do not use real confidential data for inspection.

Plugins are trusted Python code with full process access, not sandboxed extensions.
Literal replacement is case-sensitive and sequential, not comprehensive PII detection.
It covers message content only, not arbitrary provider metadata or other response fields.
The upstream deadline does not impose a plugin execution deadline or guarantee that provider work stops.
Blocking plugin code blocks the event loop; use asynchronous I/O for plugin network calls.
There is no client-disconnect cancellation guarantee, rate limiting, persistence, or production deployment configuration.
