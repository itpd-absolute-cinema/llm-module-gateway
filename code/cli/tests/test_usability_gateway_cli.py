"""Offline checks for study outcomes, private logs, and the staging boundary."""

import io
import json
import os
import sys
import tempfile
import unittest
import urllib.error
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest.mock import MagicMock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import usability_gateway_cli as study


class StudyCliTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.log = Path(self.temp.name) / "events.jsonl"
        self.log.touch(mode=0o600)
        self.env = {
            "UT01_STAGING_URL": "http://staging.invalid:8081",
            "LLM_GATEWAY_URL": "http://staging.invalid:8081",
            "UT01_SESSION_ID": "00000000-0000-4000-8000-000000000001",
            "UT01_EVENT_LOG": str(self.log),
            "LLM_GATEWAY_TOKEN": "synthetic-test-token",
        }

    def invoke(self, args, payload=None, error=None, overrides=None):
        response = MagicMock()
        response.__enter__.return_value = response
        response.headers = {"Content-Type": "application/json"}
        response.read.return_value = json.dumps(payload).encode()
        env = self.env | (overrides or {})
        with patch.dict(os.environ, env, clear=True), \
                patch.object(sys, "argv", ["gw", *args]), \
                patch("urllib.request.urlopen", return_value=response,
                      side_effect=error) as send, \
                redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
            study.run()
        return send

    def events(self):
        return [json.loads(line) for line in self.log.read_text().splitlines()]

    def test_three_task_probe_effects_and_no_payload_in_log(self):
        effects = [
            ("[stub] Call +1 202 555 0100", "unmasked"),
            ("[stub] Call [PHONE]", "masked"),
            ("[stub] Call +1 202 555 0100", "unmasked"),
        ]
        for content, expected in effects:
            send = self.invoke(["probe"], {"choices": [{"message": {"content": content}}]})
            request = send.call_args.args[0]
            self.assertEqual(json.loads(request.data)["messages"][0]["content"],
                             "Call +1 202 555 0100")
            event = self.events()[-1]
            self.assertEqual(event["probe_effect"], expected)
            self.assertEqual(event["outcome"], "ok")
            self.assertEqual(event["session_id"], self.env["UT01_SESSION_ID"])
            self.assertEqual(event["source"], "usability-test")
            self.assertEqual(set(event), {"outcome", "probe_effect", "source", "session_id",
                                          "action", "timestamp", "duration_ms"})
        for private_value in [self.env["LLM_GATEWAY_TOKEN"], "staging.invalid", "555", "choices"]:
            self.assertNotIn(private_value, self.log.read_text())

    def test_enable_and_disable_are_logged_through_normal_cli(self):
        for enabled, action in [("true", "enable"), ("false", "disable")]:
            send = self.invoke(["plugins", "patch", "mask_phone", "--enabled", enabled],
                               {"name": "mask_phone", "enabled": enabled == "true"})
            request = send.call_args.args[0]
            self.assertEqual(request.get_header("Authorization"), "Bearer synthetic-test-token")
            self.assertEqual(json.loads(request.data)["enabled"], enabled == "true")
            self.assertEqual(self.events()[-1]["action"], action)
        self.assertNotIn("mask_phone", self.log.read_text())

    def test_probe_without_token_omits_authorization(self):
        send = self.invoke(["probe"], {"choices": [{"message": {"content": "unexpected"}}]},
                           overrides={"LLM_GATEWAY_TOKEN": ""})
        self.assertIsNone(send.call_args.args[0].get_header("Authorization"))
        self.assertEqual(self.events()[0]["probe_effect"], "unexpected")

    def test_failed_request_is_logged_without_exception_details(self):
        with self.assertRaises(SystemExit):
            self.invoke(["probe"], error=urllib.error.URLError("private-network-detail"))
        self.assertEqual(self.events()[0]["outcome"], "error")
        self.assertEqual(self.events()[0]["action"], "probe")
        self.assertNotIn("private-network-detail", self.log.read_text())

    def test_host_override_cannot_send_outside_configured_staging(self):
        with patch("urllib.request.urlopen") as send:
            with self.assertRaises(SystemExit):
                with patch.dict(os.environ, self.env, clear=True), \
                        patch.object(sys, "argv", ["gw", "--host", "http://other.invalid",
                                                   "plugins", "list"]), \
                        redirect_stderr(io.StringIO()):
                    study.run()
            send.assert_not_called()
        self.assertEqual(self.events(), [])


if __name__ == "__main__":
    unittest.main()
