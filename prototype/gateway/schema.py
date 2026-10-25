from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, Field, model_validator


class ErrorDetail(BaseModel):
    message: str
    type: Literal["gateway_error"]
    param: None
    code: str


class ErrorResponse(BaseModel):
    error: ErrorDetail


def error_response(description: str, *, unauthorized: bool = False) -> dict:
    headers = {
        "X-Request-ID": {
            "description": "Identifier for this request.",
            "schema": {"type": "string"},
        },
    }
    if unauthorized:
        headers["WWW-Authenticate"] = {
            "description": "Bearer authentication is required.",
            "schema": {"type": "string", "const": "Bearer"},
        }
    return {"model": ErrorResponse, "description": description, "headers": headers}


class Message(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    role: Literal["system", "developer", "user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    model: str = Field(min_length=1)
    messages: list[Message] = Field(min_length=1)
    stream: Literal[False] = False
    temperature: float | None = Field(default=None, ge=0, le=2)
    top_p: float | None = Field(default=None, ge=0, le=1)
    max_tokens: int | None = Field(default=None, gt=0)
    max_completion_tokens: int | None = Field(default=None, gt=0)
    stop: str | list[str] | None = None

    @model_validator(mode="after")
    def one_token_limit(self) -> Self:
        if self.max_tokens is not None and self.max_completion_tokens is not None:
            raise ValueError("Specify only one output-token limit")
        return self
