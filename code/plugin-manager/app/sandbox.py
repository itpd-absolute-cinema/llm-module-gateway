"""Выполнение плагинов в отдельных процессах с жёстким таймаутом.

Зачем процессы, а не потоки: поток в Python убить нельзя, а типичное зависание плагина —
катастрофический regex (`re` держит GIL) или бесконечный цикл — блокирует и таймер, и весь
сервис. Процесс можно убить всегда.

Устройство: пул долгоживущих рабочих процессов (spawn). Каждый умеет загрузить любой плагин
(с кэшем по mtime, как раньше) и принимает вызовы по каналу. Если плагин не уложился во время —
процесс убивается (SIGKILL), вместо него в фоне поднимается новый, а плагин пропускается.
Время ожидания свободного процесса в таймаут плагина не входит.

Рабочий процесс импортирует только plugin_api и app.loader (без FastAPI/pydantic) — быстро стартует.
"""
import logging
import multiprocessing
import queue
import threading
import time
import traceback
from pathlib import Path

log = logging.getLogger("plugin-manager")

START_TIMEOUT = 20.0


class PluginTimeout(Exception):
    """Плагин не уложился в отведённое время; его процесс убит."""


class PluginBusy(Exception):
    """Не нашлось свободного рабочего процесса за отведённое время ожидания."""


class PluginCrashed(Exception):
    """Рабочий процесс умер во время вызова (падение интерпретатора, OOM, sys.exit и т. п.)."""


# ---------------------------------------------------------------- дочерний процесс

def _serve(conn, log_level: str) -> None:
    import logging as logging_
    logging_.basicConfig(level=log_level, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

    from plugin_api import Forbidden, Metrics, PluginReject
    from app import loader

    conn.send(("ready",))
    while True:
        try:
            msg = conn.recv()
        except (EOFError, OSError):
            return
        if msg is None:
            return
        _, plugins_dir, name, kind, options, args = msg
        metrics = Metrics()
        try:
            loader.PLUGINS_DIR = Path(plugins_dir)
            plugin = loader.load_plugin(name, kind, options)
            plugin.metrics = metrics
            reply = ("ok", plugin.apply(*args), metrics.ops)
        except PluginReject as exc:
            reply = ("reject", isinstance(exc, Forbidden), str(exc), metrics.ops)
        except (Exception, SystemExit):
            reply = ("error", traceback.format_exc(), metrics.ops)
        try:
            conn.send(reply)
        except Exception:  # результат нельзя сериализовать
            conn.send(("error", "plugin result cannot be sent:\n" + traceback.format_exc(), []))


# ---------------------------------------------------------------- родительский процесс

class _Worker:
    def __init__(self, ctx, log_level: str) -> None:
        self.conn, child = ctx.Pipe()
        self.proc = ctx.Process(target=_serve, args=(child, log_level), daemon=True)
        self.proc.start()
        child.close()
        try:
            if not self.conn.poll(START_TIMEOUT) or self.conn.recv() != ("ready",):
                raise RuntimeError("sandbox worker did not start")
        except (EOFError, OSError, RuntimeError):
            self.kill()
            raise RuntimeError("sandbox worker did not start") from None

    def call(self, msg, timeout: float):
        try:
            self.conn.send(msg)
        except (BrokenPipeError, OSError):
            raise PluginCrashed() from None
        if not self.conn.poll(timeout):
            raise PluginTimeout()
        try:
            return self.conn.recv()
        except (EOFError, OSError):
            raise PluginCrashed() from None

    def kill(self) -> None:
        try:
            self.proc.kill()
            self.proc.join(2)
        except Exception:  # noqa: BLE001
            pass
        try:
            self.conn.close()
        except Exception:  # noqa: BLE001
            pass


class Sandbox:
    def __init__(self, size: int = 4, log_level: str = "INFO") -> None:
        self.size = max(1, size)
        self.log_level = log_level
        self._ctx = multiprocessing.get_context("spawn")
        self._idle: "queue.Queue[_Worker]" = queue.Queue()
        self._workers: set[_Worker] = set()
        self._lock = threading.Lock()
        self._started = False
        self._closed = False

    def start(self) -> None:
        """Поднимает пул в фоне; идемпотентно. Вызовы до готовности процессов просто подождут в очереди."""
        with self._lock:
            if self._started or self._closed:
                return
            self._started = True
        for _ in range(self.size):
            self._spawn_async()

    def close(self) -> None:
        with self._lock:
            self._closed = True
            workers = list(self._workers)
        for w in workers:
            w.kill()

    def run(self, name: str, kind: str, options: dict, args: tuple, *, timeout: float,
            queue_timeout: float, plugins_dir: Path):
        """Выполняет plugin.apply(*args) в рабочем процессе.

        Возвращает ответ процесса: ("ok", result, ops) | ("reject", forbidden, message, ops) | ("error", traceback, ops).
        Бросает PluginBusy / PluginTimeout / PluginCrashed.
        """
        self.start()
        try:
            worker = self._idle.get(timeout=queue_timeout)
        except queue.Empty:
            raise PluginBusy() from None
        try:
            reply = worker.call(("call", str(plugins_dir), name, kind, options, args), timeout)
        except (PluginTimeout, PluginCrashed):
            self._discard(worker)
            raise
        except BaseException:
            self._discard(worker)
            raise
        self._idle.put(worker)
        return reply

    # ---- внутреннее ----

    def _discard(self, worker: _Worker) -> None:
        worker.kill()
        with self._lock:
            self._workers.discard(worker)
        if not self._closed:
            self._spawn_async()

    def _spawn_async(self) -> None:
        threading.Thread(target=self._spawn, name="sandbox-spawn", daemon=True).start()

    def _spawn(self) -> None:
        for attempt in range(1, 4):
            if self._closed:
                return
            try:
                worker = _Worker(self._ctx, self.log_level)
            except Exception:  # noqa: BLE001
                if self._closed:  # остановка сервиса убила стартующий процесс — это не ошибка
                    return
                log.exception("sandbox worker failed to start (attempt %d/3)", attempt)
                time.sleep(1.0)
                continue
            with self._lock:
                if self._closed:
                    worker.kill()
                    return
                self._workers.add(worker)
            self._idle.put(worker)
            return
        log.error("sandbox: could not replace a worker; the pool is smaller until the next restart")
