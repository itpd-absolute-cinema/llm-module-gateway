import re

from plugin_api import RequestFilter

# слова «книга», «книги», «книгу», «книжный»… (без учёта регистра)
BOOK = re.compile(r"\bкни(?:г|ж)\w*", re.IGNORECASE)


class BookCounter(RequestFilter):
    """Считает запросы к LLM со словом «книга». Ничего не меняет в запросе."""

    description = "Считает запросы со словом «книга»"
    metrics_spec = {
        "book_requests": {"type": "counter", "help": "Запросы к LLM со словом «книга»"},
    }

    def apply(self, request: dict) -> dict:
        text = " ".join(m["content"] for m in request["messages"])
        if BOOK.search(text):
            self.metrics.inc("book_requests")
        return request
