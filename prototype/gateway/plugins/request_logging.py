import asyncio
import logging
from time import perf_counter

from ..core import Context, Handler, Response


logger = logging.getLogger("gateway")


def factory(next_handler: Handler) -> Handler:
    async def handle(ctx: Context) -> Response:
        started = perf_counter()
        status = 500
        exception_class = None
        try:
            response = await next_handler(ctx)
            status = response.status
            return response
        except asyncio.CancelledError as exc:
            status = 499
            exception_class = type(exc).__name__
            raise
        except Exception as exc:
            exception_class = type(exc).__name__
            raise
        finally:
            logger.info(
                "request_id=%s status=%s elapsed_ms=%.3f exception_class=%s",
                ctx.request_id,
                status,
                (perf_counter() - started) * 1000,
                exception_class,
            )

    return handle
