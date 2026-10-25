import os
import tomllib
from functools import partial
from importlib import import_module
from pathlib import Path

from pydantic import BaseModel, ConfigDict, Field, HttpUrl

from gateway.core import Plugin


class Upstream(BaseModel):
    model_config = ConfigDict(extra="forbid")

    base_url: HttpUrl
    api_key_env: str = "UPSTREAM_API_KEY"
    timeout_seconds: float = Field(default=30, gt=0)


class PluginConfig(BaseModel):
    model_config = ConfigDict(extra="forbid")

    factory: str
    enabled: bool = True
    options: dict = Field(default_factory=dict)


class Config(BaseModel):
    model_config = ConfigDict(extra="forbid")

    upstream: Upstream
    plugins: list[PluginConfig]


def config_path() -> Path:
    return Path(os.environ.get("GATEWAY_CONFIG", "config.toml")).resolve()


def load(path: Path | None = None) -> Config:
    path = path or config_path()
    with path.open("rb") as file:
        return Config.model_validate(tomllib.load(file))


def secret(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise ValueError(f"Missing environment variable: {name}")
    return value


def factories(config: Config) -> list[Plugin]:
    plugins = []
    for item in config.plugins:
        if not item.enabled:
            continue
        module, name = item.factory.rsplit(":", 1)
        factory = getattr(import_module(module), name)
        plugins.append(partial(factory, **item.options))
    return plugins
