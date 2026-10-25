from collections.abc import Awaitable, Callable, Sequence
from dataclasses import dataclass, field
from typing import Any


@dataclass
class Context:
    body: dict[str, Any]
    request_id: str
    authorization: str | None = field(default=None, repr=False)


@dataclass
class Response:
    body: dict[str, Any]
    status: int = 200


def error(status: int, message: str, code: str) -> Response:
    return Response(
        {"error": {"message": message, "type": "gateway_error", "param": None, "code": code}},
        status,
    )


Handler = Callable[[Context], Awaitable[Response]]
Plugin = Callable[[Handler], Handler]


def compose(terminal: Handler, plugins: Sequence[Plugin]) -> Handler:
    handler = terminal
    for plugin in reversed(plugins):
        handler = plugin(handler)
        if not callable(handler):
            raise TypeError("Plugin factory must return a handler")
    return handler
