import logging
import re

from plugin_api import RequestFilter

log = logging.getLogger("plugin.mask_ip")

OCTET = r"(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"
IPV4 = re.compile(rf"\b{OCTET}(?:\.{OCTET}){{3}}\b")


class MaskIp(RequestFilter):
    """Маскирует IPv4-адреса. options: {"mask": "[IP]"}"""

    def apply(self, request: dict) -> dict:
        mask = self.options.get("mask", "[IP]")
        for i, msg in enumerate(request["messages"]):
            msg["content"], n = IPV4.subn(mask, msg["content"])
            if n:
                log.info("masked %d IP address(es) in message #%d (role=%s)", n, i, msg["role"])
        return request
