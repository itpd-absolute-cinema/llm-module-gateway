import logging
import re

from plugin_api import RequestFilter

log = logging.getLogger("plugin.mask_phone")

PHONE = re.compile(r"\+?\d[\d\s().-]{8,}\d")


class MaskPhone(RequestFilter):
    """Маскирует телефоны в сообщениях. options: {"mask": "[PHONE]"}"""

    def apply(self, request: dict) -> dict:
        mask = self.options.get("mask", "[PHONE]")
        total = 0
        for i, msg in enumerate(request["messages"]):
            msg["content"], n = PHONE.subn(mask, msg["content"])
            if n:
                total += n
                log.info("masked %d phone(s) in message #%d (role=%s)", n, i, msg["role"])
        log.debug("done, masked total=%d, model=%s", total, request["model"])
        return request