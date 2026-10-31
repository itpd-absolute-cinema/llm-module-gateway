import logging

from plugin_api import InvalidData, RequestFilter

log = logging.getLogger("plugin.limit_input_length")


class LimitInputLength(RequestFilter):
    """Отклоняет слишком длинные запросы (400). options: {"max_chars": 8000}"""

    def apply(self, request: dict) -> dict:
        max_chars = int(self.options.get("max_chars", 8000))
        total = sum(len(m["content"]) for m in request["messages"])
        if total > max_chars:
            log.warning("request rejected: %d chars > %d", total, max_chars)
            raise InvalidData(f"request is too long: {total} chars, limit is {max_chars}")
        log.debug("length ok: %d/%d chars", total, max_chars)
        return request
