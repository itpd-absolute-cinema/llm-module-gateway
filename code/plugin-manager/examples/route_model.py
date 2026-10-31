import logging

from plugin_api import RequestFilter

log = logging.getLogger("plugin.route_model")


class RouteModel(RequestFilter):
    """Переключает длинные запросы на модель с большим контекстом.
    options: {"threshold_chars": 4000, "long_model": "gpt-x-long"}"""

    def apply(self, request: dict) -> dict:
        long_model = self.options.get("long_model")
        if not long_model:
            log.warning("option 'long_model' is not set, skipping")
            return request
        threshold = int(self.options.get("threshold_chars", 4000))
        total = sum(len(m["content"]) for m in request["messages"])
        if total > threshold and request["model"] != long_model:
            log.info("routed %s -> %s (%d chars > %d)", request["model"], long_model, total, threshold)
            request["model"] = long_model
        return request
