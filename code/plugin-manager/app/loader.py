"""Загрузка плагинов из PLUGINS_DIR/<name>.py с кэшем по mtime.

Код плагина исполняется только в процессах песочницы (app/sandbox.py). Основной процесс
читает из файла лишь декларации метрик — через ast, без исполнения кода.
"""
import ast
import importlib.util
import inspect
import logging
import os
import sys
import threading
from pathlib import Path
from types import ModuleType
from typing import Any

from plugin_api import RequestFilter, ResponseFilter

log = logging.getLogger("plugin-manager")

PLUGINS_DIR = Path(os.getenv("PLUGINS_DIR", "/data/plugins"))
BASES = {"request_filter": RequestFilter, "response_filter": ResponseFilter}

_cache: dict[str, tuple[int, ModuleType]] = {}
_lock = threading.Lock()


def _plugin_path(name: str) -> Path:
    path = (PLUGINS_DIR / f"{name}.py").resolve()
    if path.parent != PLUGINS_DIR.resolve():
        raise ValueError(f"bad plugin path for {name!r}")
    return path


def _load_module(name: str) -> ModuleType:
    path = _plugin_path(name)
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


# ---- декларации метрик: читаем без исполнения кода ----

_decl_cache: dict[str, tuple[int, Any]] = {}


def declared_metrics(name: str) -> Any:
    """Сырая декларация метрик плагина (dict) или {}.

    Ищет литерал `metrics_spec` в теле класса плагина, затем `METRICS` на уровне модуля.
    Кэшируется по mtime файла.
    """
    try:
        path = _plugin_path(name)
        mtime = path.stat().st_mtime_ns
    except (OSError, ValueError):
        return {}
    cached = _decl_cache.get(name)
    if cached and cached[0] == mtime:
        return cached[1]
    spec: Any = {}
    try:
        spec = _extract_spec(ast.parse(path.read_bytes()))
    except (SyntaxError, ValueError, OSError) as exc:
        log.warning("plugin %s: cannot read the metrics declaration: %s", name, exc)
    _decl_cache[name] = (mtime, spec)
    return spec


def _assigned(body: list[ast.stmt], names: set[str]) -> Any:
    for node in body:
        if isinstance(node, ast.Assign):
            targets = [t.id for t in node.targets if isinstance(t, ast.Name)]
            value = node.value
        elif isinstance(node, ast.AnnAssign) and isinstance(node.target, ast.Name) and node.value is not None:
            targets, value = [node.target.id], node.value
        else:
            continue
        if names.intersection(targets):
            return ast.literal_eval(value)  # ValueError, если это не литерал
    return None


def _extract_spec(tree: ast.Module) -> Any:
    for node in tree.body:
        if isinstance(node, ast.ClassDef):
            bases = {getattr(b, "id", getattr(b, "attr", "")) for b in node.bases}
            if bases & {"RequestFilter", "ResponseFilter"}:
                found = _assigned(node.body, {"metrics_spec"})
                if found is not None:
                    return found
    found = _assigned(tree.body, {"METRICS"})
    return found if found is not None else {}
