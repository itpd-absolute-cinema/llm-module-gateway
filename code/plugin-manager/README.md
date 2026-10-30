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
|---|---|
| Plugin returns data | The data is passed to the next plugin |
| Plugin raises `InvalidData` | The chain is interrupted, response is **400** |
| Plugin raises `Forbidden` | The chain is interrupted, response is **403** |
| File is missing, code/import error, any other exception, invalid result, or incorrect plugin type | Plugin is **skipped** (an error is logged), and the data continues unchanged |
| Invalid request body | **422** (treated as an "other error" by the Gateway, resulting in 502) |

All errors are returned using the `ErrorResponse` format defined in OpenAPI.

Each plugin receives a **copy** of the data, so a failed plugin cannot partially corrupt the original data.

## Running the Service

Environment variables:

| Variable | Default | Description |
|---|---|---|
| `PLUGINS_DIR` | `/data/plugins` | Directory containing plugins (shared volume) |
| `LOG_LEVEL` | `INFO` | Logging level |

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

The service listens on port `8000`.

- Health check: `GET /health`
- Interactive API documentation: `/docs`

## Project Structure

```text
plugin_api.py     Plugin interface (RequestFilter, ResponseFilter, InvalidData, Forbidden)
app/main.py       Endpoints and plugin chain execution logic
app/loader.py     Plugin loading from PLUGINS_DIR
app/schemas.py    Data models based on OpenAPI
examples/         Example plugins
PLUGINS.md        Guide to writing plugins
```

## Important Notes

- Plugins are **executable code** running inside the service process. Only place trusted code in the shared volume. The volume is mounted read-only, and the service runs as user `uid 10001`. Plugin files must therefore be readable by all users.
- Plugins have access to the Python standard library and packages installed in the image (currently `fastapi`, `uvicorn`, and `pydantic`). To use additional dependencies, add them to `requirements.txt` and rebuild the image.
- There is no execution timeout for plugins. They must be fast and must not perform blocking network calls.
- All plugins within a single request are executed sequentially. Requests are processed concurrently (using a thread pool). Therefore, plugins must not maintain mutable state between calls unless proper locking is implemented.
- A new plugin instance is created for every call.