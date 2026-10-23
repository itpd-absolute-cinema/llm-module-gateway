import hmac
import os

from ..core import Context, Handler, Response, error


def factory(next_handler: Handler, *, key_env: str = "GATEWAY_API_KEY") -> Handler:
    key = os.environ.get(key_env)
    if not key:
        raise ValueError(key_env)
    expected = ("Bearer " + key).encode("utf-8")

    async def handle(ctx: Context) -> Response:
        supplied = (ctx.authorization or "").encode("utf-8")
        if not hmac.compare_digest(supplied, expected):
            return error(401, "Unauthorized", "unauthorized")
        return await next_handler(ctx)

    return handle
