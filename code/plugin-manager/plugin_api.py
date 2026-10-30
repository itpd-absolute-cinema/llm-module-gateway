"""Публичный интерфейс плагинов Plugin Manager.

Плагин — это .py файл, в котором определён ровно один конкретный класс-наследник
RequestFilter или ResponseFilter. Подробности — в PLUGINS.md.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Mapping

__all__ = ["RequestFilter", "ResponseFilter", "PluginReject", "InvalidData", "Forbidden"]


class PluginReject(Exception):
    """Базовое исключение «плагин отклонил данные». Само по себе не используется."""


class InvalidData(PluginReject):
    """Данные некорректны. Plugin Manager ответит 400."""


class Forbidden(PluginReject):
    """Данные запрещены (например, модель не разрешена). Plugin Manager ответит 403."""


class _Plugin(ABC):
    def __init__(self, options: Mapping[str, Any]) -> None:
        # Настройки плагина, пришедшие от гейтвея в этом вызове.
        self.options: dict[str, Any] = dict(options)


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
