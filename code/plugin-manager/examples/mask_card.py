import logging
import re

from plugin_api import RequestFilter

log = logging.getLogger("plugin.mask_card")

# 13-19 цифр, возможно разделённых пробелами или дефисами
CARD = re.compile(r"\b(?:\d[ -]?){12,18}\d\b")


def luhn_ok(digits: str) -> bool:
    total = 0
    for i, ch in enumerate(reversed(digits)):
        d = int(ch)
        if i % 2 == 1:
            d = d * 2 - 9 if d > 4 else d * 2
        total += d
    return total % 10 == 0


class MaskCard(RequestFilter):
    """Маскирует номера банковских карт (проверка по алгоритму Луна, чтобы не трогать
    обычные длинные числа). options: {"mask": "[CARD]"}"""

    def apply(self, request: dict) -> dict:
        mask = self.options.get("mask", "[CARD]")
        count = 0

        def repl(m: re.Match) -> str:
            nonlocal count
            if luhn_ok(re.sub(r"\D", "", m.group())):
                count += 1
                return mask
            return m.group()

        for i, msg in enumerate(request["messages"]):
            count = 0
            msg["content"] = CARD.sub(repl, msg["content"])
            if count:
                log.info("masked %d card number(s) in message #%d (role=%s)", count, i, msg["role"])
        return request
