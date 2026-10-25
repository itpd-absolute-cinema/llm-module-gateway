"""Replace configured literals in message text; this is not PII detection."""

from ..core import Context, Handler, Response


def factory(
    next_handler: Handler,
    *,
    literals: list[str],
    replacement: str = "[REDACTED]",
) -> Handler:
    if not isinstance(literals, list) or not literals or any(
        not isinstance(literal, str) or not literal for literal in literals
    ):
        raise ValueError("literals must be a nonempty list of nonempty strings")
    if not isinstance(replacement, str):
        raise ValueError("replacement must be a string")
    configured_literals = tuple(literals)

    def redact(content: str) -> str:
        for literal in configured_literals:
            content = content.replace(literal, replacement)
        return content

    async def handle(ctx: Context) -> Response:
        for message in ctx.body.get("messages", []):
            if isinstance(message, dict) and isinstance(message.get("content"), str):
                message["content"] = redact(message["content"])
        response = await next_handler(ctx)
        if 200 <= response.status < 300:
            for choice in response.body.get("choices", []):
                message = choice.get("message") if isinstance(choice, dict) else None
                if isinstance(message, dict) and isinstance(message.get("content"), str):
                    message["content"] = redact(message["content"])
        return response

    return handle
