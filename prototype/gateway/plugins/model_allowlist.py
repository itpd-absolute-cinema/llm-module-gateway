from ..core import Context, Handler, Response, error


def factory(next_handler: Handler, *, models: list[str]) -> Handler:
    if not isinstance(models, list) or not models or any(
        not isinstance(model, str) or not model for model in models
    ):
        raise ValueError("models must be a nonempty list of nonempty strings")
    allowed = tuple(models)

    async def handle(ctx: Context) -> Response:
        if ctx.body.get("model") not in allowed:
            return error(403, "Model not allowed", "model_not_allowed")
        return await next_handler(ctx)

    return handle
