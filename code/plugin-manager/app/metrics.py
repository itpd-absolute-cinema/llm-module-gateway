"""Метрики Plugin Manager для Prometheus.

Встроенные (на каждый плагин, без участия автора):
  llm_plugin_calls_total{plugin,type,outcome}      outcome: ok | rejected | error | timeout | busy
  llm_plugin_duration_seconds{plugin,type}         время плагина целиком (загрузка + работа + проверка)
  llm_plugin_worker_restarts_total{reason}         timeout | crash — сколько процессов песочницы пришлось убить

Пользовательские — те, что плагин объявил в `metrics_spec`. Имя в Prometheus: llm_plugin_<имя>,
к каждой метрике менеджер сам добавляет метку plugin. Всё, что не объявлено, отбрасывается,
значения меток ограничены объявленным списком (остальное -> "other"): кардинальность
ограничена по построению.
"""
import json
import logging
import math
import re
import threading
from dataclasses import dataclass, field

from prometheus_client import REGISTRY, CollectorRegistry, Counter, Gauge, Histogram

log = logging.getLogger("plugin-manager")

PREFIX = "llm_plugin_"
NAME_RE = re.compile(r"^[a-z][a-z0-9_]{0,59}$")
LABEL_RE = re.compile(r"^[a-z][a-z0-9_]{0,29}$")
RESERVED_NAMES = {"calls", "duration_seconds", "worker_restarts"}
RESERVED_SUFFIXES = ("_total", "_count", "_sum", "_bucket", "_created", "_info")
RESERVED_LABELS = {"plugin", "le", "quantile", "name", "value"}
MAX_LABELS = 4
MAX_LABEL_VALUES = 20
MAX_BUCKETS = 20
OTHER = "other"
KINDS = {"counter": "inc", "gauge": "set", "histogram": "observe"}
DEFAULT_BUCKETS = (0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)

# встроенные метрики живут в общем REGISTRY
CALLS = Counter("llm_plugin_calls", "Plugin calls by outcome", ["plugin", "type", "outcome"])
DURATION = Histogram("llm_plugin_duration_seconds", "Plugin run time including loading and validation",
                     ["plugin", "type"], buckets=(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10, 30))
RESTARTS = Counter("llm_plugin_worker_restarts", "Sandbox worker processes killed or lost", ["reason"])
for _reason in ("timeout", "crash"):
    RESTARTS.labels(reason=_reason)


def observe_call(plugin: str, kind: str, outcome: str, seconds: float) -> None:
    CALLS.labels(plugin=plugin, type=kind, outcome=outcome).inc()
    DURATION.labels(plugin=plugin, type=kind).observe(seconds)


@dataclass(frozen=True)
class Spec:
    name: str
    kind: str
    help: str
    labels: dict[str, tuple[str, ...]] = field(default_factory=dict)
    buckets: tuple[float, ...] = DEFAULT_BUCKETS

    @property
    def signature(self):
        return (self.kind, tuple(self.labels), self.buckets if self.kind == "histogram" else ())


class MetricsRegistry:
    def __init__(self, registry: CollectorRegistry = REGISTRY) -> None:
        self._registry = registry
        self._lock = threading.Lock()
        self._collectors: dict[str, tuple[tuple, object]] = {}
        self._declared: dict[tuple[str, str], dict[str, Spec]] = {}
        self._warned: set[tuple] = set()

    # ---- объявления ----

    def declare(self, plugin: str, raw) -> dict[str, Spec]:
        """Проверяет объявление и регистрирует коллекторы. Возвращает принятые метрики."""
        if not raw:
            return {}
        try:
            key = (plugin, json.dumps(raw, sort_keys=True, default=str))
        except (TypeError, ValueError):
            key = (plugin, repr(raw))
        with self._lock:
            if key in self._declared:
                return self._declared[key]
            accepted: dict[str, Spec] = {}
            if not isinstance(raw, dict):
                self._warn(plugin, "metrics_spec", "must be a dict {metric_name: {...}}")
            else:
                for name, body in raw.items():
                    spec = self._parse(plugin, name, body)
                    if spec is None:
                        continue
                    collector = self._collector(plugin, spec)
                    if collector is None:
                        continue
                    accepted[name] = spec
                    if not spec.labels:  # серия появляется сразу, с нуля — rate()/increase() не теряют первое событие
                        self._child(collector, spec, plugin, {})
            self._declared[key] = accepted
            return accepted

    def _parse(self, plugin: str, name, body) -> "Spec | None":
        def bad(reason: str):
            self._warn(plugin, str(name), reason)

        if not isinstance(name, str) or not NAME_RE.match(name):
            return bad("name must match [a-z][a-z0-9_]{0,59}")
        if name in RESERVED_NAMES or name.endswith(RESERVED_SUFFIXES):
            return bad("name is reserved or ends with a reserved suffix (_total, _count, _sum, _bucket, ...)")
        if not isinstance(body, dict) or body.get("type") not in KINDS:
            return bad("must be a dict with type: counter | gauge | histogram")
        labels: dict[str, tuple[str, ...]] = {}
        raw_labels = body.get("labels", {})
        if not isinstance(raw_labels, dict) or len(raw_labels) > MAX_LABELS:
            return bad(f"labels must be a dict {{label: [allowed values]}} with at most {MAX_LABELS} labels")
        for label, values in raw_labels.items():
            if (not isinstance(label, str) or not LABEL_RE.match(label) or label in RESERVED_LABELS
                    or not isinstance(values, (list, tuple)) or not values
                    or len(values) > MAX_LABEL_VALUES or not all(isinstance(v, (str, int, bool)) for v in values)):
                return bad(f"label {label!r}: need [a-z][a-z0-9_]*, not reserved, and 1..{MAX_LABEL_VALUES} allowed values")
            labels[label] = tuple(str(v) for v in values)
        buckets = DEFAULT_BUCKETS
        if body["type"] == "histogram" and "buckets" in body:
            b = body["buckets"]
            if (not isinstance(b, (list, tuple)) or not 1 <= len(b) <= MAX_BUCKETS
                    or not all(isinstance(x, (int, float)) and not isinstance(x, bool) and math.isfinite(x) and x > 0 for x in b)
                    or list(b) != sorted(set(b))):
                return bad(f"buckets must be 1..{MAX_BUCKETS} strictly increasing positive numbers")
            buckets = tuple(float(x) for x in b)
        help_ = body.get("help")
        return Spec(name, body["type"], help_ if isinstance(help_, str) and help_ else name, labels, buckets)

    def _collector(self, plugin: str, spec: Spec):
        existing = self._collectors.get(spec.name)
        if existing:
            if existing[0] != spec.signature:
                self._warn(plugin, spec.name, "conflicts with the declaration of the same metric (another plugin or "
                           "an earlier version of this file): type, labels and buckets cannot change without a "
                           "Plugin Manager restart")
                return None
            return existing[1]
        full, labelnames = PREFIX + spec.name, ["plugin", *spec.labels]
        if spec.kind == "counter":
            collector = Counter(full, spec.help, labelnames, registry=self._registry)
        elif spec.kind == "gauge":
            collector = Gauge(full, spec.help, labelnames, registry=self._registry)
        else:
            collector = Histogram(full, spec.help, labelnames, buckets=spec.buckets, registry=self._registry)
        self._collectors[spec.name] = (spec.signature, collector)
        return collector

    @staticmethod
    def _child(collector, spec: Spec, plugin: str, labels: dict):
        return collector.labels(plugin=plugin, **labels)

    # ---- значения от плагина ----

    def apply(self, plugin: str, declared: dict[str, Spec], ops) -> None:
        """Применяет значения, полученные от плагина. Невалидное отбрасывается с предупреждением."""
        for op in ops or ():
            try:
                kind_op, name, value, labels = op
                spec = declared.get(name)
                if spec is None:
                    self._warn(plugin, str(name), "metric is not declared in metrics_spec; value dropped")
                    continue
                if KINDS[spec.kind] != kind_op:
                    self._warn(plugin, name, f"a {spec.kind} is updated with {KINDS[spec.kind]}(), not {kind_op}()")
                    continue
                if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
                    self._warn(plugin, name, "value must be a finite number")
                    continue
                if spec.kind == "counter" and value < 0:
                    self._warn(plugin, name, "a counter cannot decrease")
                    continue
                if set(labels) != set(spec.labels):
                    self._warn(plugin, name, f"expected labels {sorted(spec.labels)}, got {sorted(labels)}")
                    continue
                values = {k: (str(v) if str(v) in spec.labels[k] else OTHER) for k, v in labels.items()}
                collector = self._collectors[name][1]
                child = collector.labels(plugin=plugin, **values)
                if spec.kind == "counter":
                    child.inc(value)
                elif spec.kind == "gauge":
                    child.set(value)
                else:
                    child.observe(value)
            except Exception as exc:  # noqa: BLE001 — метрики не должны ронять цепочку
                self._warn(plugin, "?", f"bad metric operation: {exc}")

    def _warn(self, plugin: str, metric: str, reason: str) -> None:
        key = (plugin, metric, reason)
        if key not in self._warned and len(self._warned) < 1000:  # один раз на причину, без спама в логах
            self._warned.add(key)
            log.warning("plugin %s metric %s: %s", plugin, metric, reason)


registry = MetricsRegistry()
