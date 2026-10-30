"""Pydantic-модели, соответствующие openapi.yaml."""
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

Loose = ConfigDict(extra="allow")  # неизвестные поля не теряем


class Message(BaseModel):
    model_config = Loose
    role: Literal["system", "developer", "user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    model_config = Loose
    model: str = Field(min_length=1)
    messages: list[Message] = Field(min_length=1)
    stream: Literal[False] = False
    temperature: float | None = Field(None, ge=0, le=2)
    top_p: float | None = Field(None, ge=0, le=1)
    max_tokens: int | None = Field(None, gt=0)
    max_completion_tokens: int | None = Field(None, gt=0)
    stop: str | list[str] | None = None


class Choice(BaseModel):
    model_config = Loose
    index: int | None = None
    message: Message | None = None
    finish_reason: str | None = None


class Usage(BaseModel):
    model_config = Loose
    prompt_tokens: int | None = None
    completion_tokens: int | None = None
    total_tokens: int | None = None


class ChatCompletionResponse(BaseModel):
    model_config = Loose
    id: str | None = None
    object: str | None = None
    created: int | None = None
    model: str | None = None
    choices: list[Choice] | None = None
    usage: Usage | None = None


class PluginSpec(BaseModel):
    name: str = Field(pattern=r"^[A-Za-z_][A-Za-z0-9_]*$")
    type: Literal["request_filter", "response_filter"]
    enabled: Literal[True]
    options: dict[str, Any]


class RequestFiltersInput(BaseModel):
    request: ChatRequest
    plugins: list[PluginSpec]


class ResponseFiltersInput(BaseModel):
    request: ChatRequest
    response: ChatCompletionResponse
    plugins: list[PluginSpec]
