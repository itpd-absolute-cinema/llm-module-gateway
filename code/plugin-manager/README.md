# Plugin Manager

An internal service that processes chat requests and LLM responses through a chain of filter plugins.

It is called by the LLM Gateway. The API contract is defined in [API](../../docs/api/openapi-gateway-pluginmanager.yml).

## How It Works

```text
Client → Gateway → POST /request-filters → Gateway → LLM → Gateway → POST /response-filters → Client
```

1. The Gateway sends `POST /request-filters`: the request and an ordered list of plugins (`request_filter`) with their settings.
2. The Plugin Manager executes the plugins in order. The output of each plugin becomes the input for the next one. The service returns the final processed request.
3. After receiving the LLM response, the Gateway sends `POST /response-filters`: the response, the original request (for context), and a list of `response_filter` plugins.

Plugins are not transferred over the network. Given the name `test_plugin`, the service loads the file from `<PLUGINS_DIR>/test_plugin.py`.

Plugin files are automatically reloaded when modified (no restart required). Plugin settings (`options`) are provided with every call.

### What Happens When Something Goes Wrong

| Scenario | Result |
| --- | --- |
| Plugin returns data | The data is passed to the next plugin |
| Plugin raises `InvalidData` | The chain is interrupted, response is **400** |
| Plugin raises `Forbidden` | The chain is interrupted, response is **403** |
| File is missing, code/import error, any other exception, invalid result, or incorrect plugin type | Plugin is **skipped** (an error is logged), and the data continues unchanged |
| Plugin runs longer than its time limit (`PLUGIN_TIMEOUT_SECONDS`, or `options.timeout_seconds` for that plugin) | Plugin is **skipped**, its worker process is **killed** and replaced |
| No free worker process within `PLUGIN_QUEUE_TIMEOUT_SECONDS` | Plugin is **skipped** |
| Invalid request body | **422** (treated as an "other error" by the Gateway, resulting in 502) |

All errors are returned using the `ErrorResponse` format defined in OpenAPI.

Each plugin receives a **copy** of the data, so a failed plugin cannot partially corrupt the original data.

### Time Limits

Plugins run in a pool of separate worker processes, so a hung plugin (an endless loop, a catastrophic regular expression, a stuck network call) cannot block the service: when it exceeds the limit, its process is killed and the plugin is skipped. The default limit is `PLUGIN_TIMEOUT_SECONDS`; it can be overridden for one plugin with the reserved option `timeout_seconds` in the plugin's settings in the gateway:

```json
{"name": "heavy_plugin", "type": "request_filter", "enabled": true, "options": {"timeout_seconds": 20}}
```

Skipping is fail-open: the data continues without the plugin. See [PLUGINS.md](PLUGINS.md#failures-and-time-limits).

## Metrics

`GET /metrics` returns metrics in Prometheus format: built-in metrics for every plugin (`llm_plugin_calls_total`, `llm_plugin_duration_seconds`, `llm_plugin_worker_restarts_total`) and the metrics that plugins declare themselves (`self.metrics.inc(...)`, see [PLUGINS.md](PLUGINS.md#metrics)). Prometheus scrapes the service directly; the endpoint must not be published outside the internal network.

## Running the Service

Environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `PLUGINS_DIR` | `/data/plugins` | Directory containing plugins (shared volume) |
| `LOG_LEVEL` | `INFO` | Logging level |
| `PLUGIN_TIMEOUT_SECONDS` | `5` | Default time limit of one plugin call; override per plugin with `options.timeout_seconds` |
| `PLUGIN_WORKERS` | `4` | Number of worker processes (plugin calls that can run at the same time) |
| `PLUGIN_QUEUE_TIMEOUT_SECONDS` | `5` | How long a call waits for a free worker before the plugin is skipped |

**Docker:**

```bash
docker build -t itpd/plugin-manager:latest .
```

**docker-compose:**

```yaml
plugin-manager:
  image: itpd/plugin-manager:latest
  environment:
    PLUGINS_DIR: /data/plugins
  volumes:
    - plugins:/data/plugins:ro
```

**Local development:**

```bash
pip install -r requirements.txt
PYTHONPATH=. PLUGINS_DIR=./examples uvicorn app.main:app --port 8000
```

Tests: `pip install -r requirements-dev.txt && PYTHONPATH=. python -m unittest discover -s tests -t .`

The service listens on port `8000`.

- Health check: `GET /health`
- Prometheus metrics: `GET /metrics`
- Interactive API documentation: `/docs`

## Project Structure

```text
plugin_api.py     Plugin interface (RequestFilter, ResponseFilter, Metrics, InvalidData, Forbidden)
app/main.py       Endpoints and plugin chain execution logic
app/sandbox.py    Worker process pool with a hard time limit
app/metrics.py    Prometheus metrics: built-in and plugin-declared
app/loader.py     Plugin loading from PLUGINS_DIR; metric declarations (read without running the code)
app/schemas.py    Data models based on OpenAPI
examples/         Example plugins
PLUGINS.md        Guide to writing plugins
```

## Important Notes

- Plugins are **executable code**. They run in worker processes of the service with the same privileges, so only place trusted code in the shared volume. The volume is mounted read-only, and the service runs as user `uid 10001`. Plugin files must therefore be readable by all users.
- Plugins have access to the Python standard library and packages installed in the image (currently `fastapi`, `uvicorn`, `pydantic` and `prometheus-client`). To use additional dependencies, add them to `requirements.txt` and rebuild the image.
- Plugins have a time limit (see above) but no memory limit. Limit the container's memory in compose if plugins process large data.
- All plugins within a single request are executed sequentially. Requests are processed concurrently, up to `PLUGIN_WORKERS` plugin calls at a time. Plugins must not keep state between calls: a new plugin instance is created for every call, and module-level state is per worker process.
- The worst case for one request is the sum of the time limits of its plugins; the gateway's timeout for calling the Plugin Manager must be larger.
- Run **one** uvicorn worker per instance (the default `CMD`): metrics live in the service process. To scale, run more containers; Prometheus will scrape each of them.
- The service accepts `stream: true` in the request (the gateway sends it for streaming requests); response filters are not called for streams.
