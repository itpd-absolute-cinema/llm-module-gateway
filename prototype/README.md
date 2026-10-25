# Python gateway prototype

A standalone experiment with Django-style async middleware.
It does not replace the project's Java/Python architecture decision.
Plugin administration has automated tests

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
export GATEWAY_ADMIN_API_KEY=local-demo-admin-key
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
Configuration and plugins load at startup. Admin API changes persist to TOML but do not alter the running pipeline.
Restart the gateway to apply configuration changes, whether made manually or through the API.
The API rejects writes if configuration changed on disk outside this process.
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
Entries default to `enabled = true` as it's the implied action when adding to the config.
Set `enabled = false` to retain a plugin's options without loading it.
A `.py` file in `gateway/plugins/` without a configuration entry is also disabled.
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
Factories run only at startup; handlers run per request.
The admin API never imports plugins or reruns factories, which may have side effects or allocate resources.
Put request state in `Context` or local variables, not shared factory state.
Call `next_handler` once to continue, or return without calling it to stop.
The runtime does not prevent a trusted plugin from calling it multiple times.

## Plugin administration API

The router in `gateway/admin.py` uses `Authorization: Bearer <GATEWAY_ADMIN_API_KEY>`.
Use a different value from `GATEWAY_API_KEY`. Gateway credentials do not grant admin access.
Missing or invalid admin credentials return 401; an unset admin key disables administration with 503.
Admin authorization stays active even if the gateway authentication plugin is disabled.

| Method | Path | Request / behavior |
| :--- | :--- | :--- |
| `POST` | `/admin/plugins` | Multipart `file` (`.py`), optional `options` (JSON object string), and optional `before` or `after` plugin name; creates a disabled plugin, returns 201 |
| `GET` | `/admin/plugins` | Lists installed, configured, or active local plugins alphabetically with `name`, `enabled`, `options`, and `restart_required` |
| `GET` | `/admin/plugins/{name}` | Gets one plugin's running status, options, and restart flag |
| `PUT` | `/admin/plugins/{name}` | JSON `{"options": {...}}` with optional `before` or `after`; replaces all options, preserving enabled status |
| `PATCH` | `/admin/plugins/{name}` | JSON with `enabled`, `before`, or `after`; supports order-only moves and preserves options |
| `DELETE` | `/admin/plugins/{name}` | Deletes source and configuration, returns 204; must be disabled in both saved config and the running process, otherwise 409 |

Names are Python module names without `.py`. The API manages single-file modules in
`gateway/plugins/`, including bundled plugins. Custom import paths remain supported in
TOML but are not managed through this API. Multiple entries for the same local module
must be resolved manually before that module can be managed.

GET returns status and options from the enabled startup snapshot. Inactive plugins,
including new uploads, return `enabled: false` and `options: {}`, not their saved options.
Each plugin's `restart_required` compares its presence, options (including value types),
and numeric position in the saved enabled chain with the startup chain.

The Gateway owns saved writes; administration and execution share one process in this
prototype. POST, PUT, and PATCH still return saved `{name, enabled, options}` objects,
without a `restart_required` body field. There is no separate desired-state GET endpoint.

All successful admin responses include `X-Restart-Required: true` when the saved enabled
pipeline differs from the startup configuration, or `false` when it matches.
This header is process-wide, unlike the per-plugin GET field.
Uploading or editing a disabled plugin alone does not require a restart.
Disabling an active plugin leaves GET showing it as enabled until restart: disable,
restart, then delete.

For the `prefix.py` example above:

```sh
curl http://127.0.0.1:8000/admin/plugins \
  -H 'Authorization: Bearer local-demo-admin-key' \
  -F 'file=@prefix.py' \
  -F 'options={"prefix":"Company assistant: "}'

curl -X PATCH http://127.0.0.1:8000/admin/plugins/prefix \
  -H 'Authorization: Bearer local-demo-admin-key' \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true}'

curl -X PUT http://127.0.0.1:8000/admin/plugins/prefix \
  -H 'Authorization: Bearer local-demo-admin-key' \
  -H 'Content-Type: application/json' \
  -d '{"options":{"prefix":"Updated prefix: "}}'
```

Restart the gateway after these calls to load the enabled plugin with its new options.

Uploads are limited to 1 MiB and checked for valid Python syntax without execution.
Existing files cannot be overwritten. Disable, restart if active, and delete before uploading a replacement.
Options must be representable in TOML, so JSON `null` is rejected with 422.
Factory-specific validation happens only at startup. Saving settings or enabling a plugin
does not prove it can load: invalid options, missing dependencies, or missing secrets can
prevent the next startup. Correct the saved settings through the still-running API before
restarting, or edit TOML manually if startup has already failed.

Without an ordering hint, new entries append and existing entries keep their position.
Use `before` or `after` with a configured local plugin name (without `.py`) to place an
entry immediately next to it. Disabled entries participate in this order. For example:

```sh
curl -X PATCH http://127.0.0.1:8000/admin/plugins/prefix \
  -H 'Authorization: Bearer local-demo-admin-key' \
  -H 'Content-Type: application/json' \
  -d '{"before":"literal_redaction"}'
```

Creation accepts the same hints as multipart fields, such as `-F 'after=authentication'`.
PUT still requires `options`; PATCH accepts an order-only request or combines a move
with `enabled`. JSON `null` hints are treated as omitted. Both hints together or a
self-reference return 400; a target without a configuration entry returns 404;
multiple entries for the target return 409. Options/status and placement are saved together.
Hints perform a one-time move, not a persistent dependency. Repeating the same move
without intervening changes leaves the configuration unchanged. GET listings remain
alphabetical, not execution order. Restart to apply changes to the enabled chain.
Successful mutations save TOML with an atomic file replacement, preserving unrelated
configuration and comments. Changing an option may replace comments attached to that option.
The config file, its parent directory, and the plugin directory must be writable.
All chat requests keep using the startup pipeline until the process restarts.

Run **one worker** when using this API: its mutation lock and saved-state tracking are
process-local. Do not edit files concurrently with API calls. Upstream changes and manual
plugin edits also require a restart.

Admin access allows uploading executable Python, reading plugin options, and disabling
request authentication. Treat it as full process access, restrict network exposure, and
use HTTPS outside localhost. Plugins are not sandboxed. Store secret references in options,
not literal credentials. The upload limit applies to the source file after multipart parsing;
use a reverse-proxy request-body limit for exposed deployments.

Run the tests without an upstream server:

```sh
uv run python -m unittest discover -s tests -v
```

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
There is no client-disconnect cancellation guarantee, rate limiting, request-data persistence, or production deployment configuration.
