import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from app.main import run_chain
from app.schemas import ChatRequest, PluginSpec
from plugin_api import InvalidData


class LoggingTest(unittest.TestCase):
    def test_failing_plugin_is_logged_and_skipped(self):
        request = {"model": "demo", "messages": [{"role": "user", "content": "hello"}]}
        spec = PluginSpec(name="failing", type="request_filter", enabled=True, options={})
        with TemporaryDirectory() as directory:
            Path(directory, "failing.py").write_text(
                "from plugin_api import RequestFilter\n"
                "class Failing(RequestFilter):\n"
                "    def apply(self, request):\n"
                "        raise ValueError('deliberate failure')\n"
            )
            with patch("app.loader.PLUGINS_DIR", Path(directory)), self.assertLogs("plugin-manager") as logs:
                result = run_chain("request_filter", request, [spec], ChatRequest, lambda plugin, data: plugin.apply(data))
        self.assertEqual(result, request)
        self.assertTrue(any("plugin failing failed, skipped" in line for line in logs.output))
        self.assertTrue(any("plugin failing stage=request_filter duration_ms=" in line for line in logs.output))

    def test_duration_is_logged_for_success_failure_and_rejection(self):
        request = {"model": "demo", "messages": [{"role": "user", "content": "hello"}]}
        spec = PluginSpec(name="example", type="request_filter", enabled=True, options={})

        for outcome in (None, ValueError("failure"), InvalidData("rejected")):
            with self.subTest(outcome=outcome), patch("app.main.load_plugin") as load:
                def apply(_, data):
                    if outcome:
                        raise outcome
                    return data

                with self.assertLogs("plugin-manager", level="INFO") as logs:
                    if isinstance(outcome, InvalidData):
                        with self.assertRaises(InvalidData):
                            run_chain("request_filter", request, [spec], ChatRequest, apply)
                    else:
                        self.assertEqual(run_chain("request_filter", request, [spec], ChatRequest, apply), request)
                self.assertTrue(any("plugin example stage=request_filter duration_ms=" in line for line in logs.output))
                if isinstance(outcome, ValueError):
                    self.assertTrue(any("plugin example failed, skipped" in line for line in logs.output))
                load.assert_called_once()


if __name__ == "__main__":
    unittest.main()
