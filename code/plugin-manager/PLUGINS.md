# How to Write Plugins

A plugin is a regular Python file located in `PLUGINS_DIR`. The filename must match the plugin name: the gateway sends `test_plugin`, and the service loads `test_plugin.py`. Names must contain only Latin letters, digits, and `_`, and must not start with a digit.

## Two Types of Plugins

| Type (`type`) | Base class | Method | Called |
| --- | --- | --- | --- |
| `request_filter` | `RequestFilter` | `apply(self, request: dict) -> dict` | Before the LLM call |
| `response_filter` | `ResponseFilter` | `apply(self, response: dict, request: dict) -> dict` | After the LLM call |

File requirements:

1. Import the base class from `plugin_api`.
2. Define **exactly one** concrete class inheriting from the appropriate base class.
3. Implement `apply`. The plugin type configured in the gateway must match its base class.

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
- `stream` is always `false`.
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

## Configuration

`self.options` is a dictionary containing the plugin settings provided by the gateway **for the current call**:

```json
{
  "name": "mask_phone",
  "type": "request_filter",
  "enabled": true,
  "options": {
    "mask": "***"
  }
}
```

Always use `self.options.get(key, default_value)` because settings may be missing.

## Rejecting Data

```python
from plugin_api import InvalidData, Forbidden

raise InvalidData("error message")  # Client receives HTTP 400
raise Forbidden("error message")    # Client receives HTTP 403
```

The processing chain is interrupted, and the remaining plugins are not executed. The error text is placed in `error.message`, prefixed with the plugin name.

**Any other exception** means that the plugin has failed. It is skipped, and processing continues with the data unchanged by that plugin.

Use `InvalidData` / `Forbidden` only when you explicitly want to reject the request.

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

- `model_allowlist.py` – returns HTTP 403 for models that are not allowed.
- `strip_18plus.py` – an example of a response filter.

## Recommendations

- Keep plugins fast and deterministic. Avoid long-running network calls (there is no timeout).
- Do not store state between calls. A new instance is created for every call, and requests are processed concurrently.
- The standard library and packages included in the image are available. Any additional dependencies must be added to the image.
- The gateway controls plugin execution order. Run data-masking plugins before filters that inspect message content.
- Plugin files are automatically reloaded when changed. Check the service logs for plugin errors (`plugin <name> failed, skipped`).
- To test a plugin locally, run:

```bash
PYTHONPATH=. PLUGINS_DIR=./examples uvicorn app.main:app
```

Then use `/docs` to send a `POST /request-filters` request.
