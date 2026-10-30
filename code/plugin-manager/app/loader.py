"""Загрузка плагинов из PLUGINS_DIR/<name>.py с кэшем по mtime."""
import importlib.util
import inspect
import os
import sys
import threading
from pathlib import Path
from types import ModuleType

from plugin_api import RequestFilter, ResponseFilter

PLUGINS_DIR = Path(os.getenv("PLUGINS_DIR", "/data/plugins"))
BASES = {"request_filter": RequestFilter, "response_filter": ResponseFilter}

_cache: dict[str, tuple[int, ModuleType]] = {}
_lock = threading.Lock()


def _load_module(name: str) -> ModuleType:
    path = (PLUGINS_DIR / f"{name}.py").resolve()
    if path.parent != PLUGINS_DIR.resolve():
        raise ValueError(f"bad plugin path for {name!r}")
    mtime = path.stat().st_mtime_ns  # FileNotFoundError, если файла нет
    with _lock:
        cached = _cache.get(name)
        if cached and cached[0] == mtime:
            return cached[1]
        modname = f"_plugin_{name}"
        spec = importlib.util.spec_from_file_location(modname, path)
        module = importlib.util.module_from_spec(spec)
        sys.modules[modname] = module  # нужно, например, для dataclasses
        try:
            spec.loader.exec_module(module)
        except BaseException:
            sys.modules.pop(modname, None)
            raise
        _cache[name] = (mtime, module)
        return module


def load_plugin(name: str, kind: str, options: dict):
    """Возвращает экземпляр плагина нужного вида, созданный с текущими options."""
    base = BASES[kind]
    module = _load_module(name)
    classes = [
        c for c in vars(module).values()
        if inspect.isclass(c) and issubclass(c, base)
        and c.__module__ == module.__name__ and not inspect.isabstract(c)
    ]
    if len(classes) != 1:
        raise TypeError(
            f"plugin {name!r} must define exactly one {base.__name__} subclass, found {len(classes)}"
        )
    return classes[0](options)
