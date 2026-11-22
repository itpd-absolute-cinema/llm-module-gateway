import threading
import time
import unittest
from copy import deepcopy
from pathlib import Path
from unittest.mock import patch

from app import main
from app.main import run_chain
from app.sandbox import Sandbox
from app.schemas import ChatCompletionResponse, ChatRequest
from plugin_api import Forbidden, InvalidData

from .helpers import REQUEST, RESPONSE, PluginDir, request_plugin, sample, spec

EXAMPLES = Path(__file__).resolve().parent.parent / "examples"


class ChainTestCase(unittest.TestCase):
    pool_size = 2

    @classmethod
    def setUpClass(cls):
        cls.sandbox = Sandbox(size=cls.pool_size)
        cls._patches = [patch.object(main, "sandbox", cls.sandbox)]
        for p in cls._patches:
            p.start()

    @classmethod
    def tearDownClass(cls):
        for p in cls._patches:
            p.stop()
        cls.sandbox.close()

    def setUp(self):
        self.dir = PluginDir()
        self.addCleanup(self.dir.cleanup)
        p = patch("app.loader.PLUGINS_DIR", self.dir.path)
        p.start()
        self.addCleanup(p.stop)

    def chain(self, *specs, data=None):
        return run_chain("request_filter", deepcopy(data or REQUEST), list(specs), ChatRequest)


class BehaviourTest(ChainTestCase):
    def test_plugin_changes_data_and_next_plugin_sees_it(self):
        self.dir.write("upper", request_plugin("""
            request["messages"][0]["content"] = request["messages"][0]["content"].upper()
            return request"""))
        self.dir.write("suffix", request_plugin("""
            request["messages"][0]["content"] += "!"
            return request"""))
        result = self.chain(spec("upper"), spec("suffix"))
        self.assertEqual(result["messages"][0]["content"], "HELLO!")

    def test_plugin_can_route_the_model(self):
        self.dir.write("route", request_plugin("""
            request["model"] = "big"
            return request"""))
        self.assertEqual(self.chain(spec("route"))["model"], "big")

    def test_failing_plugin_is_logged_and_skipped(self):
        self.dir.write("failing", request_plugin("raise ValueError('deliberate failure')"))
        with self.assertLogs("plugin-manager") as logs:
            result = self.chain(spec("failing"))
        self.assertEqual(result, REQUEST)
        self.assertTrue(any("plugin failing failed, skipped" in line for line in logs.output))
        self.assertTrue(any("deliberate failure" in line for line in logs.output))  # traceback из процесса плагина
        self.assertTrue(any("plugin failing stage=request_filter duration_ms=" in line for line in logs.output))

    def test_missing_plugin_and_invalid_result_are_skipped(self):
        self.dir.write("empty_messages", request_plugin("""
            request["messages"] = []
            return request"""))
        with self.assertLogs("plugin-manager"):
            result = self.chain(spec("ghost"), spec("empty_messages"))
        self.assertEqual(result, REQUEST)

    def test_rejections_interrupt_the_chain(self):
        self.dir.write("bad", request_plugin("raise InvalidData('too long')", "from plugin_api import InvalidData"))
        self.dir.write("nope", request_plugin("raise Forbidden('model is not allowed')", "from plugin_api import Forbidden"))
        self.dir.write("after", request_plugin("raise AssertionError('must not run')"))
        with self.assertRaises(InvalidData) as cm:
            self.chain(spec("bad"), spec("after"))
        self.assertEqual((str(cm.exception), cm.exception.plugin), ("too long", "bad"))
        with self.assertRaises(Forbidden):
            self.chain(spec("nope"))

    def test_response_filter_receives_the_request(self):
        self.dir.write("echo", """
            from plugin_api import ResponseFilter
            class P(ResponseFilter):
                def apply(self, response, request):
                    response["choices"][0]["message"]["content"] += " / " + request["model"]
                    return response
        """)
        result = run_chain("response_filter", deepcopy(RESPONSE), [spec("echo", "response_filter")],
                           ChatCompletionResponse, request=REQUEST)
        self.assertEqual(result["choices"][0]["message"]["content"], "hi / demo")

    def test_plugin_cannot_corrupt_data_by_failing_halfway(self):
        self.dir.write("half", request_plugin("""
            request["messages"][0]["content"] = "changed"
            raise RuntimeError("boom")"""))
        with self.assertLogs("plugin-manager"):
            self.assertEqual(self.chain(spec("half")), REQUEST)


class TimeoutTest(ChainTestCase):
    HANG = "while True:\n    pass"

    def test_hung_plugin_is_skipped_and_the_chain_continues(self):
        self.dir.write("hang", request_plugin(self.HANG))
        self.dir.write("after", request_plugin("""
            request["model"] = "after"
            return request"""))
        before = sample("llm_plugin_worker_restarts_total", reason="timeout")
        calls = lambda: sample("llm_plugin_calls_total", plugin="hang", type="request_filter", outcome="timeout")
        calls_before = calls()
        started = time.monotonic()
        with self.assertLogs("plugin-manager") as logs:
            result = self.chain(spec("hang", timeout_seconds=0.5), spec("after"))
        self.assertLess(time.monotonic() - started, 4)
        self.assertEqual(result["model"], "after")
        self.assertTrue(any("plugin hang timed out after 0.5s" in line for line in logs.output))
        self.assertEqual(sample("llm_plugin_worker_restarts_total", reason="timeout"), before + 1)
        self.assertEqual(calls(), calls_before + 1)

    def test_catastrophic_regex_is_killed(self):
        """re держит GIL: поток-таймер такой плагин не остановил бы, процесс — останавливает."""
        self.dir.write("redos", request_plugin(
            "re.match(r'(a+)+$', 'a' * 60 + 'b')\nreturn request", "import re"))
        started = time.monotonic()
        with self.assertLogs("plugin-manager"):
            result = self.chain(spec("redos", timeout_seconds=0.5))
        self.assertEqual(result, REQUEST)
        self.assertLess(time.monotonic() - started, 4)

    def test_default_timeout_and_per_plugin_override(self):
        self.dir.write("slow", request_plugin("""
            time.sleep(1.0)
            request["model"] = "slow-done"
            return request""", "import time"))
        with patch.object(main, "DEFAULT_TIMEOUT", 0.4), self.assertLogs("plugin-manager") as logs:
            skipped = self.chain(spec("slow"))
        self.assertEqual(skipped["model"], "demo")
        self.assertTrue(any("timed out after 0.4s" in line for line in logs.output))
        with patch.object(main, "DEFAULT_TIMEOUT", 0.4):
            done = self.chain(spec("slow", timeout_seconds=5))
        self.assertEqual(done["model"], "slow-done")

    def test_timeout_option_is_not_passed_to_the_plugin(self):
        self.dir.write("opts", request_plugin("""
            request["messages"][0]["content"] = repr(sorted(self.options))
            return request"""))
        result = self.chain(spec("opts", timeout_seconds=3, mask="*"))
        self.assertEqual(result["messages"][0]["content"], "['mask']")

    def test_invalid_timeout_option_falls_back_to_the_default(self):
        self.dir.write("fine", request_plugin("""
            request["model"] = "ok"
            return request"""))
        for bad in (-1, 0, "abc", True, None, 10_000):
            with self.subTest(value=bad), self.assertLogs("plugin-manager") as logs:
                self.assertEqual(self.chain(spec("fine", timeout_seconds=bad))["model"], "ok")
            self.assertTrue(any("timeout_seconds" in line and "invalid" in line for line in logs.output))

    def test_crashing_worker_is_replaced(self):
        self.dir.write("die", request_plugin("os._exit(1)", "import os"))
        self.dir.write("fine", request_plugin("""
            request["model"] = "ok"
            return request"""))
        before = sample("llm_plugin_worker_restarts_total", reason="crash")
        with self.assertLogs("plugin-manager") as logs:
            result = self.chain(spec("die"), spec("fine"))
        self.assertEqual(result["model"], "ok")
        self.assertTrue(any("crashed" in line for line in logs.output))
        self.assertEqual(sample("llm_plugin_worker_restarts_total", reason="crash"), before + 1)

    def test_pool_survives_many_timeouts(self):
        self.dir.write("hang", request_plugin(self.HANG))
        self.dir.write("fine", request_plugin("""
            request["model"] = "ok"
            return request"""))
        with self.assertLogs("plugin-manager"):
            for _ in range(4):  # больше, чем воркеров в пуле
                self.chain(spec("hang", timeout_seconds=0.3))
        self.assertEqual(self.chain(spec("fine"))["model"], "ok")


class BusyPoolTest(ChainTestCase):
    pool_size = 1

    def test_plugin_is_skipped_when_no_worker_is_free(self):
        self.dir.write("slow", request_plugin("""
            time.sleep(1.5)
            return request""", "import time"))
        self.dir.write("quick", request_plugin("""
            request["model"] = "quick"
            return request"""))
        busy = threading.Thread(target=lambda: self.chain(spec("slow", timeout_seconds=5)))
        busy.start()
        time.sleep(0.5)  # воркер занят
        before = sample("llm_plugin_calls_total", plugin="quick", type="request_filter", outcome="busy")
        with patch.object(main, "QUEUE_TIMEOUT", 0.3), self.assertLogs("plugin-manager") as logs:
            result = self.chain(spec("quick"))
        busy.join()
        self.assertEqual(result["model"], "demo")
        self.assertTrue(any("no free sandbox worker" in line for line in logs.output))
        self.assertEqual(sample("llm_plugin_calls_total", plugin="quick", type="request_filter", outcome="busy"), before + 1)


class MetricsFlowTest(ChainTestCase):
    def test_book_counter_example(self):
        (self.dir.path / "book_counter.py").write_text((EXAMPLES / "book_counter.py").read_text())
        before = sample("llm_plugin_book_requests_total", plugin="book_counter")
        for text in ("Посоветуй книгу про Python", "Какие КНИГИ почитать?", "Привет", "книжный магазин рядом"):
            data = {"model": "demo", "messages": [{"role": "user", "content": text}]}
            self.assertEqual(self.chain(spec("book_counter"), data=data), data)  # запрос не меняется
        self.assertEqual(sample("llm_plugin_book_requests_total", plugin="book_counter"), before + 3)
        self.assertEqual(sample("llm_plugin_calls_total", plugin="book_counter", type="request_filter", outcome="ok"), 4)
        self.assertGreater(sample("llm_plugin_duration_seconds_count", plugin="book_counter", type="request_filter"), 0)

    def test_metric_types_and_labels(self):
        self.dir.write("multi", """
            from plugin_api import RequestFilter
            class P(RequestFilter):
                metrics_spec = {
                    "masked": {"type": "counter", "help": "masked", "labels": {"kind": ["phone", "card"]}},
                    "chars": {"type": "histogram", "buckets": [10, 100]},
                    "last_len": {"type": "gauge"},
                }
                def apply(self, request):
                    n = len(request["messages"][0]["content"])
                    self.metrics.inc("masked", 2, kind="phone")
                    self.metrics.inc("masked", kind="ip")          # нет в списке -> other
                    self.metrics.observe("chars", n)
                    self.metrics.set("last_len", n)
                    self.metrics.inc("not_declared")               # отбросится
                    return request
        """)
        with self.assertLogs("plugin-manager", level="WARNING") as logs:
            self.chain(spec("multi"))
        self.assertEqual(sample("llm_plugin_masked_total", plugin="multi", kind="phone"), 2)
        self.assertEqual(sample("llm_plugin_masked_total", plugin="multi", kind="other"), 1)
        self.assertEqual(sample("llm_plugin_chars_bucket", plugin="multi", le="10.0"), 1)
        self.assertEqual(sample("llm_plugin_last_len", plugin="multi"), 5)
        self.assertEqual(sample("llm_plugin_not_declared_total", plugin="multi"), 0)
        self.assertTrue(any("not declared" in line for line in logs.output))

    def test_metrics_of_a_rejecting_plugin_are_kept(self):
        self.dir.write("rejecter", """
            from plugin_api import RequestFilter, InvalidData
            class P(RequestFilter):
                metrics_spec = {"rejections": {"type": "counter"}}
                def apply(self, request):
                    self.metrics.inc("rejections")
                    raise InvalidData("no")
        """)
        with self.assertRaises(InvalidData):
            self.chain(spec("rejecter"))
        self.assertEqual(sample("llm_plugin_rejections_total", plugin="rejecter"), 1)
        self.assertEqual(sample("llm_plugin_calls_total", plugin="rejecter", type="request_filter", outcome="rejected"), 1)

    def test_a_metrics_bug_does_not_break_the_plugin(self):
        self.dir.write("sloppy", """
            from plugin_api import RequestFilter
            class P(RequestFilter):
                metrics_spec = {"events": {"type": "counter"}}
                def apply(self, request):
                    self.metrics.inc("events", "not a number")
                    self.metrics.inc("events", -5)
                    self.metrics.inc("events", float("nan"))
                    self.metrics.observe("events", 1)
                    request["model"] = "still-works"
                    return request
        """)
        with self.assertLogs("plugin-manager", level="WARNING"):
            result = self.chain(spec("sloppy"))
        self.assertEqual(result["model"], "still-works")
        self.assertEqual(sample("llm_plugin_events_total", plugin="sloppy"), 0)


if __name__ == "__main__":
    unittest.main()
