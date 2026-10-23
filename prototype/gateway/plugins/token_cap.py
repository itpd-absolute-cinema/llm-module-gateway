from ..core import Context, Handler, Response


def factory(
    next_handler: Handler,
    *,
    default: int = 256,
    maximum: int = 1024,
    parameter: str = "max_tokens",
) -> Handler:
    if type(default) is not int or default <= 0:
        raise ValueError("default must be a positive integer")
    if type(maximum) is not int or maximum <= 0:
        raise ValueError("maximum must be a positive integer")
    if default > maximum:
        raise ValueError("default must not exceed maximum")
    if parameter not in ("max_tokens", "max_completion_tokens"):
        raise ValueError("parameter must be max_tokens or max_completion_tokens")

    async def handle(ctx: Context) -> Response:
        if "max_tokens" in ctx.body:
            selected = "max_tokens"
        elif "max_completion_tokens" in ctx.body:
            selected = "max_completion_tokens"
        else:
            selected = parameter
        ctx.body[selected] = min(ctx.body.get(selected, default), maximum)
        return await next_handler(ctx)

    return handle
