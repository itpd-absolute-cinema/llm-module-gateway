"""Публичный интерфейс плагинов Plugin Manager.

Плагин — это .py файл, в котором определён ровно один конкретный класс-наследник
RequestFilter или ResponseFilter. Подробности — в PLUGINS.md.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Mapping

__all__ = ["RequestFilter", "ResponseFilter", "Metrics",
           "PluginReject", "InvalidData", "Forbidden"]


class PluginReject(Exception):
    """Базовое исключение «плагин отклонил данные». Само по себе не используется."""


class InvalidData(PluginReject):
    """Данные некорректны. Plugin Manager ответит 400."""


class Forbidden(PluginReject):
    """Данные запрещены (например, модель не разрешена). Plugin Manager ответит 403."""


class Metrics:
    """Метрики плагина: `self.metrics` внутри плагина.

    Метрику нужно объявить в атрибуте класса `metrics_spec` (см. PLUGINS.md), иначе
    её значения будут отброшены. Методы никогда не бросают исключений: ошибка в метриках
    не должна ломать плагин.

        self.metrics.inc("book_requests")                  # counter
        self.metrics.set("queue_size", 12)                 # gauge
        self.metrics.observe("answer_chars", 840)          # histogram
        self.metrics.inc("masked", 3, kind="phone")        # метка (значение из объявленных)

    Сами значения копятся в `ops` и передаются менеджеру вместе с результатом вызова.
    В юнит-тестах плагина `ops` можно проверить напрямую.
    """

    MAX_OPS = 1000  # защита от плагина, который сыплет метриками в цикле

    def __init__(self) -> None:
        self.ops: list[tuple[str, str, Any, dict[str, Any]]] = []

    def inc(self, name: str, value: float = 1, **labels: Any) -> None:
        self._add("inc", name, value, labels)

    def set(self, name: str, value: float, **labels: Any) -> None:
        self._add("set", name, value, labels)

    def observe(self, name: str, value: float, **labels: Any) -> None:
        self._add("observe", name, value, labels)

    def _add(self, op: str, name: str, value: Any, labels: dict[str, Any]) -> None:
        try:
            if len(self.ops) < self.MAX_OPS:
                self.ops.append((op, str(name), value, {str(k): v for k, v in labels.items()}))
        except Exception:  # noqa: BLE001 — метрики не должны ломать плагин
            pass


class _NoMetrics(Metrics):
    """Запасной вариант для плагинов, переопределивших __init__ без super().__init__()."""

    def _add(self, *args: Any, **kwargs: Any) -> None:
        return None


class _Plugin(ABC):
    metrics: Metrics = _NoMetrics()

    def __init__(self, options: Mapping[str, Any]) -> None:
        # Настройки плагина, пришедшие от гейтвея в этом вызове
        # (без зарезервированного ключа timeout_seconds — его обрабатывает менеджер).
        self.options: dict[str, Any] = dict(options)
        self.metrics = Metrics()


class RequestFilter(_Plugin):
    """Фильтр запроса (type = request_filter)."""

    @abstractmethod
    def apply(self, request: dict) -> dict:
        """Принимает chat-запрос (dict), возвращает его же (изменённым или нет)."""


class ResponseFilter(_Plugin):
    """Фильтр ответа LLM (type = response_filter)."""

    @abstractmethod
    def apply(self, response: dict, request: dict) -> dict:
        """Принимает ответ LLM (dict) и запрос, ушедший в LLM (только для контекста).
        Возвращает ответ (изменённый или нет)."""
