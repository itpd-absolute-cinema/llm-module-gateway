# How to Write Plugins

A plugin is a regular Python file located in `PLUGINS_DIR`. The filename must match the plugin name: the gateway sends `test_plugin`, and the service loads `test_plugin.py`. Names must contain only Latin letters, digits, and `_`, and must not start with a digit.

A plugin can do three things:

| What | How | Where it is described |
| --- | --- | --- |
| **Change the request** before it goes to the LLM (messages, model, parameters) | `RequestFilter.apply(request)` | [Request filters](#request-filters) |
| **Change the LLM response** before it goes to the client | `ResponseFilter.apply(response, request)` | [Response filters](#response-filters) |
| **Publish metrics** (Prometheus / Grafana) | `self.metrics.inc(...)` | [Metrics](#metrics) |

Besides that, a plugin can reject data (`InvalidData` / `Forbidden`) and is protected against hanging by a [time limit](#failures-and-time-limits).

## Two Types of Plugins

| Type (`type`) | Base class | Method | Called |
| --- | --- | --- | --- |
| `request_filter` | `RequestFilter` | `apply(self, request: dict) -> dict` | Before the LLM call |
| `response_filter` | `ResponseFilter` | `apply(self, response: dict, request: dict) -> dict` | After the LLM call |

File requirements:

1. Import the base class from `plugin_api`.
2. Define **exactly one** concrete class inheriting from the appropriate base class.
3. Implement `apply`. The plugin type configured in the gateway must match its base class.

All plugins of one kind run **sequentially**, in the order set in the gateway: the output of a plugin is the input of the next one. Request filters see the request as changed by the previous request filters; response filters see the response as changed by the previous response filters.

## Input and Output

**Request filter** receives and returns a chat request (a dictionary):

```json
{
  "model": "gpt-x",
  "messages": [{"role": "user", "content": "..."}],
  "temperature": 0.7,
  "top_p": 1.0,
  "max_tokens": 100,
  "max_completion_tokens": null,
  "stop": null,
  "stream": false
}
```

- `model` – non-empty string. Changing it allows you to route the request to a different model.
- `messages` – non-empty list. `role` must be one of `system | developer | user | assistant`, and `content` must be a string.
- `temperature` must be between 0 and 2; `top_p` between 0 and 1; `max_tokens` and `max_completion_tokens` must be greater than 0 (or `null`).
- `stop` must be a string, a list of strings, or `null`.
- `stream` is `true` when the client asked for a streaming response. Do not change it.
- Optional fields may be absent. Unknown fields are preserved – do not remove them unless necessary.

**Response filter** receives the LLM response and the original request. The request is read-only; any modifications to it are ignored.

```json
{
  "id": "chatcmpl-1",
  "object": "chat.completion",
  "created": 1730000000,
  "model": "gpt-x",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 5,
    "total_tokens": 15
  }
}
```

Always return **the same format**. After each plugin runs, its output is validated against the schema. If the result is invalid (for example, `messages` is empty or contains an unknown `role`), the plugin is considered to have failed and is skipped.

You can modify data in place and return the same object – you are working with a copy.

## Request Filters

A request filter changes what is sent to the LLM. Typical uses:

**Edit the messages** – mask personal data, remove or add messages:

```python
import re
from plugin_api import RequestFilter

PHONE = re.compile(r"\+?\d[\d\s().-]{8,}\d")


class MaskPhone(RequestFilter):
    def apply(self, request: dict) -> dict:
        mask = self.options.get("mask", "[PHONE]")
        for msg in request["messages"]:
            msg["content"] = PHONE.sub(mask, msg["content"])
        return request
```

```python
# add a system instruction in front of the conversation
request["messages"].insert(0, {"role": "system", "content": "Answer in Russian."})
```

**Change the model** – route the request (see `examples/route_model.py`):

```python
if sum(len(m["content"]) for m in request["messages"]) > 4000:
    request["model"] = "gpt-x-long"
```

The gateway bills the request to the model it finally sends to the LLM, i.e. the one set by the last request filter.

**Change generation parameters** – `temperature`, `top_p`, `max_tokens`, `max_completion_tokens`, `stop`:

```python
request["max_tokens"] = min(request.get("max_tokens") or 1000, 1000)
```

**Reject the request** – see [Rejecting Data](#rejecting-data).

## Response Filters

A response filter changes what the client receives. It gets the response and, **for context only**, the request that went to the LLM.

```python
from plugin_api import ResponseFilter


class Disclaimer(ResponseFilter):
    def apply(self, response: dict, request: dict) -> dict:
        text = self.options.get("text", "AI-generated answer.")
        for choice in response.get("choices") or []:
            msg = choice.get("message")
            if msg and msg.get("content"):
                msg["content"] += f"\n\n{text} (model: {request['model']})"
        return response
```

- You can change `choices[].message.content`, `finish_reason`, etc. The `choices` and `message` fields may be absent or `null` – check before use.
- Changing `usage` does **not** affect accounting: the gateway records the token counts reported by the LLM.
- Raising `InvalidData` / `Forbidden` blocks the answer: the client gets an error instead. The LLM has already been called, so the tokens are still counted.
- Response filters **do not run for streaming requests** (`stream: true`). By default the gateway rejects a streaming request while any response filter is enabled.

## Configuration

`self.options` is a dictionary containing the plugin settings provided by the gateway **for the current call**:

```json
{
  "name": "mask_phone",
  "type": "request_filter",
  "enabled": true,
  "options": {
    "mask": "***",
    "timeout_seconds": 2
  }
}
```

Always use `self.options.get(key, default_value)` because settings may be missing.

**Reserved option `timeout_seconds`** – the time limit of this plugin (see [Failures and Time Limits](#failures-and-time-limits)). The manager handles it itself and removes it from `self.options`, so do not use this name for your own settings.

### Description and options schema (optional)

The gateway admin UI can show a description and build a settings form from a JSON Schema. Declare them as **literals** in the plugin class (they are read from the source without running the code):

```python
class MaskPhone(RequestFilter):
    description = "Masks phone numbers in messages"
    options_schema = {
        "type": "object",
        "properties": {
            "mask": {"type": "string", "default": "[PHONE]"},
            "timeout_seconds": {"type": "number", "minimum": 0.1, "default": 5},
        },
    }
```

Without `description` the first line of the class docstring is used. `options_schema` must be a dict that can be serialized to JSON, otherwise the gateway rejects the upload.

## Rejecting Data

```python
from plugin_api import InvalidData, Forbidden

raise InvalidData("error message")  # Client receives HTTP 400
raise Forbidden("error message")    # Client receives HTTP 403
```

The processing chain is interrupted, and the remaining plugins are not executed. The error text is placed in `error.message`, prefixed with the plugin name. Metrics recorded before the exception are kept.

**Any other exception** means that the plugin has failed. It is skipped, and processing continues with the data unchanged by that plugin.

Use `InvalidData` / `Forbidden` only when you explicitly want to reject the request.

## Failures and Time Limits

| What happened | Result |
| --- | --- |
| `apply` returned valid data | The data goes to the next plugin |
| `InvalidData` / `Forbidden` | The chain stops, the client gets 400 / 403 |
| Any other exception, import error, invalid result | The plugin is **skipped**, the error and traceback are logged |
| The plugin ran **longer than its time limit** | The plugin is **skipped**, its process is **killed** |
| All sandbox processes are busy for too long | The plugin is **skipped** (`outcome="busy"`) |

**Time limit.** By default a plugin may run for `PLUGIN_TIMEOUT_SECONDS` (5 s unless the service is configured otherwise). To change it for one plugin, set `timeout_seconds` in its options (a number greater than 0 and at most 600; an invalid value is logged and the default is used):

```json
{"name": "heavy_plugin", "type": "request_filter", "enabled": true, "options": {"timeout_seconds": 20}}
```

Things to know:

- The limit covers **everything** the manager does for the plugin: loading the file and its imports (the first call in each worker process), `apply`, and passing the data. Keep imports light or raise the limit for plugins with heavy dependencies.
- A hung plugin cannot be interrupted politely, so the process running it is killed (`SIGKILL`) and replaced. A catastrophic regular expression or `while True` is therefore harmless to the service.
- Skipping is **fail-open**: the data continues **without** this plugin. If a masking or moderation plugin times out, the data goes on unmasked / unchecked. Keep such plugins fast, give them a sensible limit, and alert on `llm_plugin_calls_total{outcome="timeout"}`.
- The worst case for one request is the sum of the limits of all plugins in the chain. The gateway has its own timeout for the call to the Plugin Manager – keep it larger than that sum.

## Metrics

A plugin can publish its own metrics. The Plugin Manager exposes them in Prometheus format at `GET /metrics`; Prometheus scrapes that endpoint and Grafana draws the charts. Nothing is sent to the gateway.

### 1. Declare

Declare every metric in the class attribute `metrics_spec` (a literal dict; a module-level `METRICS` is also accepted):

```python
class BookCounter(RequestFilter):
    metrics_spec = {
        "book_requests": {"type": "counter", "help": "Requests to the LLM that mention a book"},
        "answer_chars": {"type": "histogram", "help": "Answer length", "buckets": [100, 500, 2000, 8000]},
        "masked": {"type": "counter", "labels": {"kind": ["phone", "card", "ip"]}},
    }
```

| Key | Meaning |
| --- | --- |
| `type` | `counter` (only grows: events, items), `gauge` (current value), `histogram` (distribution: sizes, durations) |
| `help` | Description shown in Prometheus (optional) |
| `labels` | Optional dict `{label_name: [allowed values]}`, up to 4 labels with up to 20 values each |
| `buckets` | Histogram only: strictly increasing positive numbers, up to 20 (default: 0.005 … 10) |

Metric names: `[a-z][a-z0-9_]*`, up to 60 characters. They must not end with `_total`, `_count`, `_sum`, `_bucket`, `_created`, `_info` (Prometheus adds suffixes itself), and `calls`, `duration_seconds`, `worker_restarts` are taken by built-in metrics. Label names must not be `plugin`, `le`, `quantile`, `name`, `value`.

### 2. Record

`self.metrics` is available in `apply`:

```python
self.metrics.inc("book_requests")              # counter += 1
self.metrics.inc("masked", 3, kind="phone")    # counter += 3, with a label
self.metrics.set("queue_size", 12)             # gauge = 12
self.metrics.observe("answer_chars", 840)      # histogram observation
```

Recording never raises exceptions: a mistake in metrics is logged by the manager and does not break the plugin.

### What you get in Prometheus

- The name is `llm_plugin_<name>`; counters get the `_total` suffix: `book_requests` → `llm_plugin_book_requests_total`.
- The manager adds the label `plugin="<plugin name>"` to every series. Do not put it into `labels`.
- **Everything not declared is dropped** (with a warning, once). A label value that is not in the declared list is recorded as `other`. This keeps the number of series bounded: never use user ids, request ids, or free text as label values – declare closed sets.
- If two plugins declare the same metric name, the declarations must be identical (then the series differ by `plugin`). Changing the type, labels or buckets of an already registered metric needs a Plugin Manager restart; until then the new declaration is ignored with a warning.

### Built-in metrics (no code needed)

| Metric | Labels | Meaning |
| --- | --- | --- |
| `llm_plugin_calls_total` | `plugin`, `type`, `outcome` | Calls; `outcome` = `ok`, `rejected`, `error`, `timeout`, `busy` |
| `llm_plugin_duration_seconds` | `plugin`, `type` | Histogram of plugin time (loading + work + validation) |
| `llm_plugin_worker_restarts_total` | `reason` | Sandbox processes replaced: `timeout`, `crash` |

### Example: requests that mention a book

```python
import re

from plugin_api import RequestFilter

BOOK = re.compile(r"\bкни(?:г|ж)\w*", re.IGNORECASE)


class BookCounter(RequestFilter):
    description = "Counts requests that mention a book"
    metrics_spec = {"book_requests": {"type": "counter", "help": "Requests that mention a book"}}

    def apply(self, request: dict) -> dict:
        if BOOK.search(" ".join(m["content"] for m in request["messages"])):
            self.metrics.inc("book_requests")
        return request
```

PromQL:

```text
rate(llm_plugin_book_requests_total{plugin="book_counter"}[5m])                      # per second
increase(llm_plugin_book_requests_total{plugin="book_counter"}[1h])                  # per hour
increase(llm_plugin_book_requests_total[1h]) / increase(llm_plugin_calls_total{plugin="book_counter",outcome="ok"}[1h])   # share of requests
```

### What a metric means

- The plugin counts only requests that **reached it**. A request rejected by an earlier plugin, or from a blocked user, never gets there. To count every request that goes to the LLM, put the counting plugin last in the chain.
- Values recorded by a call that timed out or crashed are lost (the process was killed).
- Metrics live in the Plugin Manager process: counters start from zero after a restart (this is normal for Prometheus: `rate()` and `increase()` handle resets). Run **one** uvicorn worker per Plugin Manager instance.
- Response filters do not run for streaming requests, so their metrics do not include streams.

### Testing metrics in a unit test

Outside the manager `self.metrics` just collects the calls in `ops`:

```python
plugin = BookCounter({})
plugin.apply({"model": "m", "messages": [{"role": "user", "content": "Посоветуй книгу"}]})
assert plugin.metrics.ops == [("inc", "book_requests", 1, {})]
```

## How Plugins Are Run

Every call is executed in a separate **worker process** (a pool of `PLUGIN_WORKERS` processes, started with `spawn`). This is what makes the time limit enforceable. Consequences for the code:

- Data is passed by copy (pickling). A plugin that fails halfway cannot corrupt the data.
- Module-level variables are **not shared** between workers and are lost when a worker is killed or restarted. Do not keep state between calls. A new plugin instance is created for every call.
- Do not start threads or subprocesses that outlive `apply`.
- `print` and `logging` work: output goes to the service log. Use `logging.getLogger("plugin.<name>")`.
- `sys.exit()` / `os._exit()` count as a failed plugin (the worker is replaced).
- Plugin files are reloaded automatically when changed (checked on every call).

## Example: Phone Number Masking (`mask_phone.py`)

```python
import re
from plugin_api import RequestFilter

PHONE = re.compile(r"\+?\d[\d\s().-]{8,}\d")


class MaskPhone(RequestFilter):
    def apply(self, request: dict) -> dict:
        mask = self.options.get("mask", "[PHONE]")
        for msg in request["messages"]:
            msg["content"] = PHONE.sub(mask, msg["content"])
        return request
```

More examples are available in the `examples/` directory:

- `book_counter.py` – a metric: counts requests that mention a book.
- `model_allowlist.py` – returns HTTP 403 for models that are not allowed.
- `route_model.py` – routes long requests to another model.
- `strip_18plus.py` – an example of a response filter.

## Recommendations

- Keep plugins fast and deterministic. Avoid network calls; if you need one, set a timeout in the call itself and a matching `timeout_seconds`.
- Do not store state between calls.
- The standard library and packages included in the image are available. Any additional dependencies must be added to the image.
- The gateway controls plugin execution order. Run data-masking plugins before filters that inspect message content.
- Check the service logs for plugin problems: `plugin <name> failed, skipped` (with traceback) and `plugin <name> timed out after ...s, skipped`.
- Each attempted plugin logs its name, stage, duration in milliseconds and outcome (`ok | rejected | error | timeout | busy`). The measurement includes loading, execution, and result validation.
- To test a plugin locally, run:

```bash
PYTHONPATH=. PLUGINS_DIR=./examples uvicorn app.main:app
```

Then use `/docs` to send a `POST /request-filters` request and look at `/metrics`.
