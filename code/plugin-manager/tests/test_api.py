import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

from app import main
from app.sandbox import Sandbox

from .helpers import REQUEST, RESPONSE, PluginDir, request_plugin


class ApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sandbox = Sandbox(size=2)
        cls.patch = patch.object(main, "sandbox", cls.sandbox)
        cls.patch.start()
        cls.client = TestClient(main.app)
        cls.client.__enter__()  # lifespan: запускает пул

    @classmethod
    def tearDownClass(cls):
        cls.client.__exit__(None, None, None)
        cls.patch.stop()

    def setUp(self):
        self.dir = PluginDir()
        self.addCleanup(self.dir.cleanup)
        p = patch("app.loader.PLUGINS_DIR", self.dir.path)
        p.start()
        self.addCleanup(p.stop)

    def plugin(self, name, kind="request_filter", **options):
        return {"name": name, "type": kind, "enabled": True, "options": options}

    def test_request_filters_and_stream_flag(self):
        self.dir.write("upper", request_plugin("""
            request["messages"][0]["content"] = request["messages"][0]["content"].upper()
            return request"""))
        body = {"request": {**REQUEST, "stream": True}, "plugins": [self.plugin("upper")]}
        r = self.client.post("/request-filters", json=body)
        self.assertEqual(r.status_code, 200, r.text)
        self.assertEqual(r.json()["messages"][0]["content"], "HELLO")
        self.assertIs(r.json()["stream"], True)

    def test_rejections_map_to_400_and_403(self):
        self.dir.write("bad", request_plugin("raise InvalidData('too long')", "from plugin_api import InvalidData"))
        self.dir.write("nope", request_plugin("raise Forbidden('no')", "from plugin_api import Forbidden"))
        r = self.client.post("/request-filters", json={"request": REQUEST, "plugins": [self.plugin("bad")]})
        self.assertEqual((r.status_code, r.json()["error"]["code"]), (400, "plugin_invalid_data"))
        r = self.client.post("/request-filters", json={"request": REQUEST, "plugins": [self.plugin("nope")]})
        self.assertEqual((r.status_code, r.json()["error"]["code"]), (403, "plugin_forbidden"))

    def test_timeout_option_over_http(self):
        self.dir.write("hang", request_plugin("while True:\n    pass"))
        r = self.client.post("/request-filters",
                             json={"request": REQUEST, "plugins": [self.plugin("hang", timeout_seconds=0.5)]})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.json(), REQUEST)

    def test_response_filters(self):
        self.dir.write("tag", """
            from plugin_api import ResponseFilter
            class P(ResponseFilter):
                def apply(self, response, request):
                    response["choices"][0]["message"]["content"] += "!"
                    return response
        """)
        r = self.client.post("/response-filters", json={
            "request": REQUEST, "response": RESPONSE, "plugins": [self.plugin("tag", "response_filter")]})
        self.assertEqual(r.json()["choices"][0]["message"]["content"], "hi!")

    def test_prometheus_endpoint(self):
        self.dir.write("noop", request_plugin("return request"))
        self.client.post("/request-filters", json={"request": REQUEST, "plugins": [self.plugin("noop")]})
        r = self.client.get("/metrics")
        self.assertEqual(r.status_code, 200)
        self.assertTrue(r.headers["content-type"].startswith("text/plain"))
        self.assertIn('llm_plugin_calls_total{outcome="ok",plugin="noop",type="request_filter"} 1.0', r.text)
        self.assertIn("llm_plugin_duration_seconds_bucket", r.text)
        self.assertIn('llm_plugin_worker_restarts_total{reason="timeout"}', r.text)


if __name__ == "__main__":
    unittest.main()
