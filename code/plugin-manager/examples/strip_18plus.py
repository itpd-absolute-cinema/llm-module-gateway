import re

from plugin_api import ResponseFilter


class Strip18Plus(ResponseFilter):
    """Заменяет стоп-слова в ответе. options: {"words": [...], "replacement": "[removed]"}"""

    def apply(self, response: dict, request: dict) -> dict:
        words = self.options.get("words", [])
        if not words:
            return response
        pattern = re.compile("|".join(re.escape(w) for w in words), re.IGNORECASE)
        replacement = self.options.get("replacement", "[removed]")
        for choice in response.get("choices") or []:
            msg = choice.get("message")
            if msg and msg.get("content"):
                msg["content"] = pattern.sub(replacement, msg["content"])
        return response
