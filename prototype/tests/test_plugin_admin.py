import importlib
import itertools
import json
import os
import py_compile
import sys
import tempfile
import unittest
from contextlib import ExitStack, contextmanager
from pathlib import Path
from unittest.mock import AsyncMock, patch
from uuid import uuid4

import httpx
from fastapi.testclient import TestClient
from starlette.datastructures import State

import gateway.plugins
from gateway.config import load
from gateway.schema import ErrorResponse

PLUGIN_SOURCE = '''from gateway.core import Response


def factory(next_handler, *, message="default", metadata=None):
    if not isinstance(message, str):
        raise ValueError("message must be a string")

    async def handle(context):
        return Response({"message": message, "metadata": metadata or {}})

    return handle
'''

CHAT_BODY = {
    "model": "test-model",
    "messages": [{"role": "user", "content": "hello"}],
}
ADMIN_HEADERS = {"Authorization": "Bearer test-admin-key"}
GATEWAY_HEADERS = {"Authorization": "Bearer test-gateway-key"}


class PluginAdminTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # A missing implementation is an error, not a silently skipped contract.
        cls.admin = importlib.import_module("gateway.admin")
        cls.app = importlib.import_module("gateway.app").app

    def setUp(self):
        self.stack = self.enterContext(ExitStack())
        self.root = Path(self.stack.enter_context(tempfile.TemporaryDirectory()))
        self.plugin_directory = self.root / "plugins"
        self.plugin_directory.mkdir()
        self.config_path = self.root / "config.toml"
        self.config_path.write_text(
            'plugins = []\n\n'
            '[upstream]\n'
            'base_url = "http://127.0.0.1:1/v1"\n'
            'api_key_env = "TEST_UPSTREAM_API_KEY"\n'
            'timeout_seconds = 1\n',
            encoding="utf-8",
        )
        self.prefix = "admin_test_" + uuid4().hex
        self.sequence = itertools.count()
        self.stack.callback(self.clean_modules)
        self.stack.enter_context(patch.dict(os.environ, {
            "GATEWAY_CONFIG": str(self.config_path),
            "GATEWAY_ADMIN_API_KEY": "test-admin-key",
            "GATEWAY_API_KEY": "test-gateway-key",
            "TEST_UPSTREAM_API_KEY": "not-a-real-upstream-key",
        }))
        self.stack.enter_context(
            patch.object(self.admin, "PLUGIN_DIRECTORY", self.plugin_directory)
        )
        self.stack.enter_context(
            patch.object(gateway.plugins, "__path__", [str(self.plugin_directory)])
        )
        self.stack.enter_context(patch.object(self.app, "state", State()))
        self.upstream_send = self.stack.enter_context(patch.object(
            httpx.AsyncClient,
            "send",
            new_callable=AsyncMock,
            side_effect=AssertionError("Tests must not contact an upstream"),
        ))
        self.clients = self.stack.enter_context(ExitStack())
        self.restart()

    def tearDown(self):
        self.upstream_send.assert_not_called()

    def clean_modules(self):
        prefix = "gateway.plugins." + self.prefix
        for module_name in list(sys.modules):
            if module_name.startswith(prefix):
                module = sys.modules.pop(module_name)
                attribute = module_name.rsplit(".", 1)[-1]
                if getattr(gateway.plugins, attribute, None) is module:
                    delattr(gateway.plugins, attribute)
        importlib.invalidate_caches()

    def restart(self):
        self.clients.close()
        # TestClient shares this interpreter; a real process restart does not.
        self.clean_modules()
        self.client = self.clients.enter_context(TestClient(self.app))
        self.assertTrue(hasattr(self.app.state, "plugin_manager"))

    def name(self):
        return f"{self.prefix}_{next(self.sequence)}"

    def request(self, method, path="", **kwargs):
        kwargs.setdefault("headers", ADMIN_HEADERS)
        response = self.client.request(method, "/admin/plugins" + path, **kwargs)
        if 200 <= response.status_code < 300:
            self.assertIn(response.headers.get("X-Restart-Required"), ("true", "false"))
            if method == "GET":
                items = [response.json()] if path else response.json()
                for item in items:
                    self.assertEqual(set(item), {"name", "enabled", "options", "restart_required"})
                    self.assertIs(type(item["restart_required"]), bool)
            elif method in ("POST", "PUT", "PATCH"):
                self.assertEqual(set(response.json()), {"name", "enabled", "options"})
        else:
            body = response.json()
            self.assertEqual(ErrorResponse.model_validate(body, strict=True).model_dump(), body)
            route = "/admin/plugins/{name}" if path else "/admin/plugins"
            documented = self.app.openapi()["paths"][route][method.lower()]["responses"]
            self.assertIn(str(response.status_code), documented)
            self.assertTrue(response.headers.get("X-Request-ID"))
            if response.status_code == 401:
                self.assertEqual(response.headers.get("WWW-Authenticate"), "Bearer")
        return response

    def upload(self, name, source: str | bytes = PLUGIN_SOURCE, options=None, **kwargs):
        data = {} if options is None else {"options": json.dumps(options)}
        return self.request(
            "POST", files={"file": (name + ".py", source, "text/x-python")},
            data=data, **kwargs,
        )

    def assert_status(self, response, status, *, restart=None):
        self.assertEqual(response.status_code, status, response.text)
        if restart is not None:
            self.assertEqual(response.headers.get("X-Restart-Required"), str(restart).lower())

    def item(self, name):
        response = self.request("GET", "/" + name)
        self.assert_status(response, 200)
        return response.json()

    def assert_running(self, expected, *, restart):
        listing = self.request("GET")
        self.assert_status(listing, 200, restart=restart)
        self.assertEqual(listing.json(), sorted(expected, key=lambda item: item["name"]))
        for item in expected:
            response = self.request("GET", "/" + item["name"])
            self.assert_status(response, 200, restart=restart)
            self.assertEqual(response.json(), item)

    def persisted(self, name):
        return next(
            item for item in load().plugins
            if item.factory == f"gateway.plugins.{name}:factory"
        )

    def enable(self, name):
        response = self.request("PATCH", "/" + name, json={"enabled": True})
        self.assert_status(response, 200)
        return response

    def assert_chat(self, message, metadata=None, headers=None):
        response = self.client.post(
            "/v1/chat/completions", json=CHAT_BODY, headers=headers or {},
        )
        self.assert_status(response, 200)
        self.assertEqual(response.json(), {
            "message": message, "metadata": metadata or {},
        })

    @contextmanager
    def fail_config_replace(self):
        original_replace = Path.replace
        failed_paths = []

        def replace(source, target):
            if Path(target) == self.config_path:
                failed_paths.append(source)
                raise OSError("Injected config replacement failure")
            return original_replace(source, target)

        with patch.object(Path, "replace", replace):
            yield
        self.assertTrue(failed_paths, "The request must reach the config commit")
        for path in failed_paths:
            self.assertFalse(path.exists(), "Failed saves must remove temporary files")

    def test_config_write_failure_preserves_settings_state_and_live_handler(self):
        active, disabled = self.name(), self.name()
        self.assert_status(self.upload(active, options={"message": "stable"}), 201)
        self.enable(active)
        self.assert_status(self.upload(disabled, options={"message": "pending"}), 201)
        self.restart()
        before = self.config_path.read_bytes()
        expected = [self.item(active), self.item(disabled)]
        handler = self.app.state.handler
        for method, name, body in [
            ("PUT", active, {"options": {"message": "changed"}}),
            ("PATCH", active, {"enabled": False}),
            ("PATCH", disabled, {"enabled": True}),
        ]:
            with self.subTest(method=method, name=name), self.fail_config_replace():
                self.assert_status(self.request(method, "/" + name, json=body), 500)
                self.assertEqual(self.config_path.read_bytes(), before)
                self.assertEqual([self.item(active), self.item(disabled)], expected)
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat("stable")
        self.assert_status(
            self.request("PUT", "/" + active, json={"options": {"message": "recovered"}}), 200,
        )
        self.assert_chat("stable")
        self.assertIs(self.app.state.handler, handler)
        self.assert_status(self.request("GET"), 200, restart=True)
        self.restart()
        self.assert_chat("recovered")
        self.assert_status(self.request("GET"), 200, restart=False)

    def test_nested_option_types_require_restart_and_exact_reversion_clears_it(self):
        name = self.name()
        self.assert_status(self.upload(name, options={
            "metadata": {"nested": {"value": 1, "values": [1]}},
        }), 201, restart=False)
        self.enable(name)
        self.restart()
        handler = self.app.state.handler
        for value, values in ((True, [1]), (1.0, [1]), (1, [True]), (1, [1.0]), (1, [1])):
            with self.subTest(value=value, values=values):
                options = {"metadata": {"nested": {"value": value, "values": values}}}
                pending = type(value) is not int or type(values[0]) is not int
                response = self.request("PUT", "/" + name, json={"options": options})
                self.assert_status(response, 200, restart=pending)
                self.assert_status(self.request("GET"), 200, restart=pending)
                self.assert_status(self.request("GET", "/" + name), 200, restart=pending)
                self.assert_running([{
                    "name": name, "enabled": True,
                    "options": {"metadata": {"nested": {"value": 1, "values": [1]}}},
                    "restart_required": pending,
                }], restart=pending)
                # Equality alone cannot distinguish 1, True, and 1.0.
                for actual in (
                    response.json()["options"]["metadata"]["nested"],
                    self.persisted(name).options["metadata"]["nested"],
                ):
                    self.assertIs(type(actual["value"]), type(value))
                    self.assertIs(type(actual["values"][0]), type(values[0]))
                    self.assertEqual(actual, options["metadata"]["nested"])
                running = self.item(name)
                self.assertEqual(running, {
                    "name": name, "enabled": True,
                    "options": {"metadata": {"nested": {"value": 1, "values": [1]}}},
                    "restart_required": pending,
                })
                nested = running["options"]["metadata"]["nested"]
                self.assertIs(type(nested["value"]), int)
                self.assertIs(type(nested["values"][0]), int)
                self.assertIs(self.app.state.handler, handler)
                chat = self.client.post("/v1/chat/completions", json=CHAT_BODY)
                self.assert_status(chat, 200)
                live = chat.json()["metadata"]["nested"]
                self.assertIs(type(live["value"]), int)
                self.assertIs(type(live["values"][0]), int)
                self.assertEqual(live, {"value": 1, "values": [1]})
        self.assert_status(self.request("PUT", "/" + name, json={
            "options": {"metadata": {"nested": {"value": 1.0}}},
        }), 200, restart=True)
        self.restart()
        self.assert_status(self.request("GET"), 200, restart=False)
        response = self.client.post("/v1/chat/completions", json=CHAT_BODY)
        self.assert_status(response, 200)
        actual = response.json()["metadata"]["nested"]["value"]
        self.assertIs(type(actual), float)
        self.assertEqual(actual, 1.0)
        running = self.item(name)
        self.assertFalse(running["restart_required"])
        self.assertIs(type(running["options"]["metadata"]["nested"]["value"]), float)

    def test_mutations_never_import_or_call_factories_or_replace_active_handler(self):
        active, pending, removed = self.name(), self.name(), self.name()
        events = self.root / "plugin-events"
        record = f"with Path({str(events)!r}).open('a') as log: log.write"
        source = (
            "from pathlib import Path\n" + record + "('import\\n')\n"
            + PLUGIN_SOURCE.replace(
                '    if not isinstance(message, str):',
                "    " + record + "('factory\\n')\n    if not isinstance(message, str):",
            )
        )
        self.assert_status(self.upload(active, source, {"message": "stable"}), 201, restart=False)
        self.assert_status(self.enable(active), 200, restart=True)
        self.assertFalse(events.exists())
        self.restart()
        self.assertEqual(events.read_text(), "import\nfactory\n")
        handler = self.app.state.handler
        module_name = f"gateway.plugins.{active}"
        module = sys.modules[module_name]
        for name in (pending, removed):
            self.assert_status(self.upload(name, source), 201, restart=False)
        for method, name, body, restart in [
            ("PUT", active, {"options": {"message": "changed"}}, True),
            ("PATCH", active, {"enabled": False}, True),
            ("PATCH", pending, {"enabled": True}, True),
            ("PUT", removed, {"options": {"message": "disabled"}}, True),
        ]:
            with self.subTest(method=method, name=name):
                before = self.config_path.read_bytes()
                expected = self.item(name)
                pending_before = self.request("GET").headers["X-Restart-Required"]
                with self.fail_config_replace():
                    self.assert_status(self.request(method, "/" + name, json=body), 500)
                self.assertEqual(self.config_path.read_bytes(), before)
                self.assertEqual(self.item(name), expected)
                self.assertEqual(self.request("GET").headers["X-Restart-Required"], pending_before)
                self.assertEqual(events.read_text(), "import\nfactory\n")
                self.assertIs(self.app.state.handler, handler)
                self.assert_status(self.request(method, "/" + name, json=body), 200, restart=restart)
                self.assertEqual(events.read_text(), "import\nfactory\n")
                self.assertIs(self.app.state.handler, handler)
                self.assertIs(sys.modules[module_name], module)
                self.assertIs(getattr(gateway.plugins, active), module)
                self.assertNotIn(f"gateway.plugins.{pending}", sys.modules)
                self.assert_chat("stable")
        failed_upload = self.name()
        with self.fail_config_replace():
            self.assert_status(self.upload(failed_upload, source), 500)
        self.assert_status(self.upload(failed_upload, source), 201, restart=True)
        with self.fail_config_replace():
            self.assert_status(self.request("DELETE", "/" + removed), 500)
        self.assert_status(self.request("DELETE", "/" + removed), 204, restart=True)
        self.assertEqual(events.read_text(), "import\nfactory\n")
        self.assertIs(self.app.state.handler, handler)
        self.assertIs(sys.modules[module_name], module)
        self.assertIs(getattr(gateway.plugins, active), module)
        for name in (pending, removed, failed_upload):
            self.assertNotIn(f"gateway.plugins.{name}", sys.modules)
        self.assert_chat("stable")
        self.restart()
        self.assertEqual(events.read_text(), "import\nfactory\nimport\nfactory\n")
        self.assert_chat("default")
        self.assert_status(self.request("GET"), 200, restart=False)

    def test_manager_save_only_persists_and_tracks_enabled_order(self):
        first, second, disabled = self.name(), self.name(), self.name()
        for name in (first, second, disabled):
            self.assert_status(self.upload(name), 201)
        self.enable(first)
        self.enable(second)
        self.restart()
        startup = load()
        handler = self.app.state.handler
        manager = self.admin.PluginManager(startup, self.config_path)
        self.assertFalse(manager.restart_required)
        with ExitStack() as mocks:
            imports = mocks.enter_context(patch("gateway.config.import_module"))
            factories = [
                mocks.enter_context(patch.object(sys.modules[f"gateway.plugins.{name}"], "factory"))
                for name in (first, second)
            ]
            changed = startup.model_copy(deep=True)
            changed.plugins.reverse()
            manager.save(changed)
            self.assertTrue(manager.restart_required)
            self.assertEqual(load().plugins, changed.plugins)
            for name in (first, second):
                self.assertTrue(manager.running_info(name).restart_required)
            self.assertFalse(manager.running_info(disabled).restart_required)
            unchanged = startup.model_copy(deep=True)
            unchanged.plugins.insert(0, unchanged.plugins.pop())
            unchanged.plugins[0].options = {"ignored_while_disabled": True}
            manager.save(unchanged)
            self.assertFalse(manager.restart_required)
            self.assertEqual(manager.info(disabled).model_dump(), {
                "name": disabled, "enabled": False, "options": {"ignored_while_disabled": True},
            })
            self.assertEqual(manager.running_info(disabled).model_dump(), {
                "name": disabled, "enabled": False, "options": {}, "restart_required": False,
            })
            changed = unchanged.model_copy(deep=True)
            changed.plugins[1].factory = "does_not_exist:factory"
            before = self.config_path.read_bytes()
            with self.fail_config_replace(), self.assertRaises(OSError):
                manager.save(changed)
            self.assertEqual(self.config_path.read_bytes(), before)
            self.assertEqual(manager.config, unchanged)
            self.assertFalse(manager.restart_required)
            manager.save(changed)
            self.assertTrue(manager.restart_required)
            self.assertEqual(load().plugins, changed.plugins)
            manager.save(startup.model_copy(deep=True))
            self.assertFalse(manager.restart_required)
            imports.assert_not_called()
            for factory in factories:
                factory.assert_not_called()
        self.assertIs(self.app.state.handler, handler)
        self.assert_chat("default")

    def test_pending_enable_can_be_reverted_despite_disabled_option_changes(self):
        name = self.name()
        self.assert_status(self.upload(name), 201, restart=False)
        self.assert_status(self.enable(name), 200, restart=True)
        inactive = {"name": name, "enabled": False, "options": {}, "restart_required": False}
        self.assert_running([{**inactive, "restart_required": True}], restart=True)
        self.assert_status(self.request("PUT", "/" + name, json={
            "options": {"message": "desired"},
        }), 200, restart=True)
        self.assert_running([{**inactive, "restart_required": True}], restart=True)
        self.assert_status(self.request("PATCH", "/" + name, json={"enabled": False}), 200, restart=False)
        self.assert_running([inactive], restart=False)
        self.assertFalse(self.persisted(name).enabled)
        self.assertEqual(self.persisted(name).options, {"message": "desired"})
        self.assert_status(self.request("DELETE", "/" + name), 204, restart=False)

    def test_running_settings_flags_are_per_plugin_and_reset_after_restart(self):
        unaffected, changed, disabled = [self.name() for _ in range(3)]
        for name in (unaffected, changed, disabled):
            self.assert_status(self.upload(name, options={"message": name}), 201)
        self.enable(unaffected)
        self.enable(changed)
        self.restart()
        initial = [
            {"name": unaffected, "enabled": True, "options": {"message": unaffected}, "restart_required": False},
            {"name": changed, "enabled": True, "options": {"message": changed}, "restart_required": False},
            {"name": disabled, "enabled": False, "options": {}, "restart_required": False},
        ]
        self.assert_running(initial, restart=False)
        self.assert_status(self.request("PUT", "/" + disabled, json={
            "options": {"message": "disabled edit"},
        }), 200, restart=False)
        self.assertEqual(self.persisted(disabled).options, {"message": "disabled edit"})
        self.assert_running(initial, restart=False)
        for options, pending in (({"message": "new"}, True), ({"message": changed}, False)):
            with self.subTest(options=options):
                response = self.request("PUT", "/" + changed, json={"options": options})
                self.assert_status(response, 200, restart=pending)
                self.assertEqual(response.json(), {"name": changed, "enabled": True, "options": options})
                self.assertEqual(self.persisted(changed).options, options)
                self.assert_running([
                    initial[0], {**initial[1], "restart_required": pending}, initial[2],
                ], restart=pending)
        self.assert_status(self.request("PATCH", "/" + changed, json={"enabled": False}), 200, restart=True)
        self.assert_status(self.request("PUT", "/" + changed, json={
            "options": {"message": "pending disabled edit"},
        }), 200, restart=True)
        pending = [initial[0], {**initial[1], "restart_required": True}, initial[2]]
        self.assert_running(pending, restart=True)
        before = self.ordering_state()
        with self.fail_config_replace():
            self.assert_status(self.request("PUT", "/" + changed, json={
                "options": {"message": changed},
            }), 500)
        self.assertEqual(self.ordering_state(), before)
        self.assert_running(pending, restart=True)
        self.assertFalse(self.persisted(changed).enabled)
        self.assertEqual(self.persisted(changed).options, {"message": "pending disabled edit"})
        self.restart()
        self.assert_running([
            initial[0], {**initial[1], "enabled": False, "options": {}}, initial[2],
        ], restart=False)
        self.assertEqual(self.persisted(changed).options, {"message": "pending disabled edit"})

    def test_restart_flags_track_numeric_enabled_positions_not_disabled_positions(self):
        first, second, last, disabled = [self.name() for _ in range(4)]
        names = (first, second, last, disabled)
        for name in names:
            self.assert_status(self.upload(name, options={"message": name}), 201)
        for name in (first, second, last):
            self.enable(name)
        self.restart()
        initial = [
            {"name": name, "enabled": name != disabled,
             "options": {"message": name} if name != disabled else {}, "restart_required": False}
            for name in names
        ]
        for name, body, flagged in [
            (first, {"after": second}, {first, second}),
            (first, {"before": second}, set()),
            (disabled, {"before": first}, set()),
            (disabled, {"enabled": True}, set(names)),
            (disabled, {"enabled": False}, set()),
            (first, {"enabled": False}, {first, second, last}),
            (first, {"enabled": True}, set()),
        ]:
            with self.subTest(name=name, body=body):
                self.assert_status(self.request("PATCH", "/" + name, json=body), 200, restart=bool(flagged))
                self.assert_running([
                    {**item, "restart_required": item["name"] in flagged} for item in initial
                ], restart=bool(flagged))
        self.assert_status(self.request("PATCH", "/" + first, json={"after": second}), 200, restart=True)
        self.restart()
        self.assert_running(initial, restart=False)
        self.assert_plugin_order([disabled, second, first, last])

    def test_listing_includes_source_config_and_active_startup_names_alphabetically(self):
        active, configured, source_only = [self.prefix + suffix for suffix in ("_z", "_m", "_a")]
        self.assert_status(self.upload(active, options={"message": "running"}), 201)
        self.assert_status(self.upload(configured, options={"message": "saved only"}), 201)
        self.enable(active)
        self.restart()
        manager = self.app.state.plugin_manager
        config = manager.config.model_copy(deep=True)
        config.plugins = [item for item in config.plugins if item.factory != f"gateway.plugins.{active}:factory"]
        manager.save(config)
        for name in (active, configured):
            (self.plugin_directory / (name + ".py")).unlink()
        (self.plugin_directory / (source_only + ".py")).write_text(
            'raise RuntimeError("GET must not import source")\n', encoding="utf-8",
        )
        self.assert_running([
            {"name": active, "enabled": True, "options": {"message": "running"}, "restart_required": True},
            {"name": configured, "enabled": False, "options": {}, "restart_required": False},
            {"name": source_only, "enabled": False, "options": {}, "restart_required": False},
        ], restart=True)
        self.assertEqual(self.persisted(configured).options, {"message": "saved only"})
        self.assertNotIn(f"gateway.plugins.{source_only}", sys.modules)
        self.assert_chat("running")
        self.restart()
        self.assert_status(self.request("GET", "/" + active), 404)
        self.assert_running([
            {"name": configured, "enabled": False, "options": {}, "restart_required": False},
            {"name": source_only, "enabled": False, "options": {}, "restart_required": False},
        ], restart=False)

    def test_partial_source_write_failure_removes_file_and_preserves_config(self):
        original_open = Path.open
        before = self.config_path.read_bytes()
        handler = self.app.state.handler
        for failure in ("write", "close"):
            with self.subTest(failure=failure):
                name = self.name()
                path = self.plugin_directory / (name + ".py")
                partial_writes = []

                class PartialWriter:
                    def __init__(self, output):
                        self.output = output

                    def __enter__(self):
                        self.output.__enter__()
                        return self

                    def __exit__(self, exc_type, exc, traceback, *, failure=failure):
                        self.output.__exit__(exc_type, exc, traceback)
                        if failure == "close" and exc_type is None:
                            raise OSError("Injected source close failure")

                    def write(self, source, *, failure=failure, partial_writes=partial_writes, path=path):
                        written = self.output.write(source[:10])
                        self.output.flush()
                        partial_writes.append(path.read_bytes())
                        if failure == "write":
                            raise OSError("Injected partial source write failure")
                        return written

                def open_path(source, mode="r", *args, path=path, writer=PartialWriter, **kwargs):
                    output = original_open(source, mode, *args, **kwargs)
                    if source == path and mode == "xb":
                        return writer(output)
                    return output

                with patch.object(Path, "open", open_path):
                    self.assert_status(self.upload(name), 500)
                self.assertEqual(partial_writes, [PLUGIN_SOURCE.encode()[:10]])
                self.assertFalse(path.exists())
                self.assertEqual(self.config_path.read_bytes(), before)
                self.assertEqual(load().plugins, [])
                self.assertIs(self.app.state.handler, handler)
                self.assert_status(self.request("GET", "/" + name), 404)
                self.assert_status(self.upload(name), 201)
                self.assertEqual(path.read_text(), PLUGIN_SOURCE)
                self.assert_status(self.request("DELETE", "/" + name), 204)
                before = self.config_path.read_bytes()

    def test_upload_config_write_failure_removes_source_and_config_entry(self):
        name = self.name()
        before = self.config_path.read_bytes()
        handler = self.app.state.handler
        with self.fail_config_replace():
            self.assert_status(self.upload(name), 500)
        self.assertFalse((self.plugin_directory / (name + ".py")).exists())
        self.assertEqual(self.config_path.read_bytes(), before)
        self.assertIs(self.app.state.handler, handler)
        self.assertEqual(load().plugins, [])
        self.assert_status(self.request("GET", "/" + name), 404)
        self.assert_status(self.upload(name), 201)
        self.enable(name)
        self.restart()
        self.assert_chat("default")

    def test_delete_config_write_failure_restores_source_and_config(self):
        name, active = self.name(), self.name()
        self.assert_status(self.upload(name, options={"message": "restored"}), 201)
        self.assert_status(self.upload(active, options={"message": "stable"}), 201)
        self.enable(active)
        self.restart()
        before = self.config_path.read_bytes()
        source_path = self.plugin_directory / (name + ".py")
        source = source_path.read_bytes()
        expected = self.item(name)
        handler = self.app.state.handler
        entries = set(self.plugin_directory.iterdir())
        with self.fail_config_replace():
            self.assert_status(self.request("DELETE", "/" + name), 500)
        self.assertEqual(source_path.read_bytes(), source)
        self.assertEqual(self.config_path.read_bytes(), before)
        self.assertEqual(self.item(name), expected)
        self.assertEqual(set(self.plugin_directory.iterdir()), entries)
        self.assertIs(self.app.state.handler, handler)
        self.assert_chat("stable")
        self.restart()
        self.assertEqual(self.item(name), expected)
        self.assert_chat("stable")
        self.enable(name)
        self.assert_chat("stable")
        self.restart()
        self.assert_chat("restored")

    def test_external_config_edit_conflicts_without_overwriting_or_mutating_state(self):
        active, disabled, uploaded = self.name(), self.name(), self.name()
        self.assert_status(self.upload(active, options={"message": "stable"}), 201)
        self.enable(active)
        self.assert_status(self.upload(disabled), 201)
        self.restart()
        external = self.config_path.read_bytes() + b"\n# External operator edit\n"
        self.config_path.write_bytes(external)
        handler = self.app.state.handler
        expected = [self.item(active), self.item(disabled)]
        source = (self.plugin_directory / (disabled + ".py")).read_bytes()
        for method, path, kwargs in [
            ("PUT", "/" + active, {"json": {"options": {"message": "changed"}}}),
            ("PATCH", "/" + active, {"json": {"enabled": False}}),
            ("POST", "", {"files": {"file": (uploaded + ".py", PLUGIN_SOURCE)}}),
            ("DELETE", "/" + disabled, {}),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.request(method, path, **kwargs), 409)
                self.assertEqual(self.config_path.read_bytes(), external)
                self.assertEqual([self.item(active), self.item(disabled)], expected)
                self.assertEqual((self.plugin_directory / (disabled + ".py")).read_bytes(), source)
                self.assertFalse((self.plugin_directory / (uploaded + ".py")).exists())
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat("stable")
        self.restart()
        self.assert_status(
            self.request("PUT", "/" + active, json={"options": {"message": "reloaded"}}), 200,
        )
        self.assertIn(b"# External operator edit", self.config_path.read_bytes())
        self.assert_chat("stable")
        self.restart()
        self.assert_chat("reloaded")

    def test_mutations_preserve_upstream_comments_and_plugin_order(self):
        first, second, added = self.prefix + "_z", self.prefix + "_a", self.name()
        upstream = (
            '# Operator configuration\n'
            '[upstream] # Keep upstream settings\n'
            'base_url = "http://127.0.0.1:1/v1" # No network requests\n'
            'api_key_env = "TEST_UPSTREAM_API_KEY"\n'
            'timeout_seconds = 1.25 # Custom timeout\n\n'
        )
        self.config_path.write_text(
            upstream
            + f'[[plugins]] # First in request order\n'
            f'factory = "gateway.plugins.{first}:factory"\n'
            'enabled = true\n'
            '[plugins.options]\n'
            'message = "first"\n\n'
            '[[plugins]] # Second in request order\n'
            f'factory = "gateway.plugins.{second}:factory"\n'
            'enabled = false\n'
            '[plugins.options] # Retain these options\n'
            'message = "second" # Unchanged value\n',
            encoding="utf-8",
        )
        for name in (first, second):
            (self.plugin_directory / (name + ".py")).write_text(PLUGIN_SOURCE, encoding="utf-8")
        self.restart()
        expected_upstream = load().upstream
        expected_factories = [f"gateway.plugins.{name}:factory" for name in (first, second)]
        for method, path, kwargs, status in [
            ("PUT", "/" + first, {"json": {"options": {"message": "updated"}}}, 200),
            ("PATCH", "/" + second, {"json": {"enabled": True}}, 200),
            ("POST", "", {"files": {"file": (added + ".py", PLUGIN_SOURCE)}}, 201),
            ("DELETE", "/" + added, {}, 204),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.request(method, path, **kwargs), status)
                text = self.config_path.read_text()
                self.assertTrue(text.startswith(upstream), text)
                for comment in ["# First in request order", "# Second in request order",
                                "# Retain these options", '# Unchanged value']:
                    self.assertIn(comment, text)
                config = load()
                self.assertEqual(config.upstream, expected_upstream)
                factories = expected_factories + ([f"gateway.plugins.{added}:factory"] if method == "POST" else [])
                self.assertEqual([item.factory for item in config.plugins], factories)
                self.assertEqual(config.plugins[1].options, {"message": "second"})
                self.assert_chat("first")
                self.assert_status(self.request("GET"), 200, restart=True)
        self.restart()
        self.assert_chat("updated")

    def test_delete_clears_bytecode_without_mutating_import_caches(self):
        name = self.name()
        source = PLUGIN_SOURCE.replace('message="default"', 'message="version-one"')
        replacement = source.replace("version-one", "version-two")
        self.assertEqual(len(source), len(replacement))
        self.assert_status(self.upload(name, source), 201, restart=False)
        path = self.plugin_directory / (name + ".py")
        timestamp = 1700000000
        os.utime(path, (timestamp, timestamp))
        bytecode_file = py_compile.compile(str(path), doraise=True)
        assert bytecode_file is not None
        bytecode = Path(bytecode_file)
        self.enable(name)
        self.restart()
        self.assert_chat("version-one")
        self.assertTrue(bytecode.exists())
        self.assert_status(self.request("PATCH", "/" + name, json={"enabled": False}), 200, restart=True)
        self.assert_status(self.request("DELETE", "/" + name), 409)
        self.assert_chat("version-one")
        self.restart()
        # A disabled module can still be imported by other code in the process.
        module_name = f"gateway.plugins.{name}"
        original_module = importlib.import_module(module_name)
        handler = self.app.state.handler
        with patch.object(importlib, "invalidate_caches") as invalidate:
            self.assert_status(self.request("DELETE", "/" + name), 204, restart=False)
            self.assertFalse(bytecode.exists())
            self.assertIs(sys.modules[module_name], original_module)
            self.assertIs(getattr(gateway.plugins, name), original_module)
            self.assert_status(self.upload(name, replacement), 201, restart=False)
            # Equal size and mtime would make stale timestamp-based bytecode valid.
            os.utime(path, (timestamp, timestamp))
            self.assert_status(self.enable(name), 200, restart=True)
            self.assertIs(sys.modules[module_name], original_module)
            self.assertIs(getattr(gateway.plugins, name), original_module)
            self.assertIs(self.app.state.handler, handler)
            invalidate.assert_not_called()
        self.restart()
        self.assertIsNot(sys.modules[module_name], original_module)
        self.assert_chat("version-two")

    def test_symlink_plugins_cannot_be_managed(self):
        name = self.name()
        target = self.root / "outside.py"
        target.write_text(PLUGIN_SOURCE, encoding="utf-8")
        link = self.plugin_directory / (name + ".py")
        link.symlink_to(target)
        before = self.config_path.read_bytes()
        for method, path, kwargs in [
            ("GET", "/" + name, {}),
            ("POST", "", {"files": {"file": (name + ".py", PLUGIN_SOURCE)}}),
            ("PUT", "/" + name, {"json": {"options": {}}}),
            ("PATCH", "/" + name, {"json": {"enabled": True}}),
            ("DELETE", "/" + name, {}),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.request(method, path, **kwargs), 400)
                self.assertTrue(link.is_symlink())
                self.assertEqual(target.read_text(), PLUGIN_SOURCE)
                self.assertEqual(self.config_path.read_bytes(), before)
        response = self.request("GET")
        self.assert_status(response, 200)
        self.assertEqual(response.json(), [])

    def test_package_initializer_cannot_be_managed(self):
        path = self.plugin_directory / "__init__.py"
        source = "# Package initializer must remain untouched\n"
        path.write_text(source, encoding="utf-8")
        before = self.config_path.read_bytes()
        for method, route, kwargs in [
            ("GET", "/__init__", {}),
            ("POST", "", {"files": {"file": ("__init__.py", PLUGIN_SOURCE)}}),
            ("PUT", "/__init__", {"json": {"options": {}}}),
            ("PATCH", "/__init__", {"json": {"enabled": True}}),
            ("DELETE", "/__init__", {}),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.request(method, route, **kwargs), 400)
                self.assertEqual(path.read_text(), source)
                self.assertEqual(self.config_path.read_bytes(), before)
        response = self.request("GET")
        self.assert_status(response, 200)
        self.assertEqual(response.json(), [])

    def test_upload_enable_update_disable_delete_and_restart(self):
        name = self.name()
        marker = self.root / "imported"
        source = (
            f"from pathlib import Path\nPath({str(marker)!r}).write_text('executed')\n"
            + PLUGIN_SOURCE
        )
        options = {
            "message": "first",
            "metadata": {"tags": ["one", "two"], "count": 2, "ratio": 1.5, "ok": True},
        }
        expected = {"name": name, "enabled": False, "options": options}
        response = self.upload(name, source, options)
        self.assert_status(response, 201, restart=False)
        self.assertEqual(response.json(), expected)
        self.assertEqual((self.plugin_directory / (name + ".py")).read_text(), source)
        self.assertFalse(marker.exists(), "Uploading must not execute plugin code")
        self.assertNotIn(f"gateway.plugins.{name}", sys.modules)
        inactive = {"name": name, "enabled": False, "options": {}, "restart_required": False}
        self.assertEqual(self.item(name), inactive)
        response = self.request("GET")
        self.assert_status(response, 200)
        self.assertEqual(response.json(), [inactive])
        self.assertFalse(self.persisted(name).enabled)
        self.assertEqual(self.persisted(name).options, options)

        self.restart()
        self.assertFalse(marker.exists(), "Disabled plugins must not execute on startup")
        self.assertEqual(self.item(name), inactive)
        self.assertEqual(self.persisted(name).options, options)
        handler = self.app.state.handler
        response = self.enable(name)
        self.assert_status(response, 200, restart=True)
        self.assertEqual(response.json(), {**expected, "enabled": True})
        self.assertFalse(marker.exists(), "Enabling must not execute plugin code")
        self.assertNotIn(f"gateway.plugins.{name}", sys.modules)
        self.assertIs(self.app.state.handler, handler)
        self.assertTrue(self.persisted(name).enabled)
        self.assertEqual(self.item(name), {**inactive, "restart_required": True})
        self.assert_status(self.request("DELETE", "/" + name), 409)
        self.restart()
        self.assertTrue(marker.exists())
        self.assert_status(self.request("GET"), 200, restart=False)
        self.assert_chat("first", options["metadata"])

        response = self.request("PUT", "/" + name, json={"options": {"message": "second"}})
        self.assert_status(response, 200)
        self.assert_status(response, 200, restart=True)
        expected = {"name": name, "enabled": True, "options": {"message": "second"}}
        self.assertEqual(response.json(), expected)
        self.assertEqual(self.persisted(name).options, {"message": "second"})
        self.assertEqual(self.item(name), {
            "name": name, "enabled": True, "options": options, "restart_required": True,
        })
        self.assert_chat("first", options["metadata"])
        self.restart()
        self.assertEqual(self.item(name), {**expected, "restart_required": False})
        self.assert_chat("second")

        config_before = self.config_path.read_bytes()
        self.assert_status(self.request("DELETE", "/" + name), 409)
        self.assertEqual(self.config_path.read_bytes(), config_before)
        self.assertTrue((self.plugin_directory / (name + ".py")).exists())
        self.assert_chat("second")

        response = self.request("PATCH", "/" + name, json={"enabled": False})
        self.assert_status(response, 200)
        expected["enabled"] = False
        self.assertEqual(response.json(), expected)
        self.assertFalse(self.persisted(name).enabled)
        self.assertEqual(self.persisted(name).options, {"message": "second"})
        self.assert_status(response, 200, restart=True)
        disabled_config = self.config_path.read_bytes()
        self.assert_status(self.request("DELETE", "/" + name), 409)
        self.assertEqual(self.config_path.read_bytes(), disabled_config)
        self.assertTrue((self.plugin_directory / (name + ".py")).exists())
        self.assert_chat("second")
        self.assertEqual(self.item(name), {**expected, "enabled": True, "restart_required": True})
        self.restart()
        self.assertEqual(self.item(name), inactive)
        self.assertEqual(self.persisted(name).options, {"message": "second"})
        response = self.request("DELETE", "/" + name)
        self.assert_status(response, 204, restart=False)
        self.assertEqual(response.content, b"")
        self.assertFalse((self.plugin_directory / (name + ".py")).exists())
        self.assertEqual(load().plugins, [])
        self.assert_status(self.request("GET", "/" + name), 404)
        self.restart()
        self.assertEqual(self.request("GET").json(), [])

    def test_default_options_and_empty_listing(self):
        response = self.request("GET")
        self.assert_status(response, 200)
        self.assertEqual(response.json(), [])
        name = self.name()
        response = self.upload(name)
        self.assert_status(response, 201)
        self.assertEqual(response.json(), {"name": name, "enabled": False, "options": {}})

    def test_unconfigured_file_is_listed_disabled_without_execution(self):
        name = self.name()
        (self.plugin_directory / (name + ".py")).write_text(
            'raise RuntimeError("must not import an unconfigured plugin")\n',
            encoding="utf-8",
        )
        config_before = self.config_path.read_bytes()
        self.restart()
        expected = {"name": name, "enabled": False, "options": {}, "restart_required": False}
        self.assertEqual(self.item(name), expected)
        response = self.request("GET")
        self.assert_status(response, 200)
        self.assertIn(expected, response.json())
        self.assertEqual(self.config_path.read_bytes(), config_before)
        self.assertNotIn(f"gateway.plugins.{name}", sys.modules)
        self.assert_status(self.request("DELETE", "/" + name), 204)
        self.assertFalse((self.plugin_directory / (name + ".py")).exists())

    def test_disabling_retains_live_handler_until_restart_and_can_be_reverted(self):
        first, fallback = self.name(), self.name()
        for name, message in [(first, "first"), (fallback, "fallback")]:
            self.assert_status(self.upload(name, options={"message": message}), 201)
            self.enable(name)
        self.restart()
        handler = self.app.state.handler
        self.assert_chat("first")
        self.assert_status(
            self.request("PATCH", "/" + first, json={"enabled": False}), 200, restart=True,
        )
        self.assert_chat("first")
        self.assertIs(self.app.state.handler, handler)
        self.assertEqual(self.item(first), {
            "name": first, "enabled": True, "options": {"message": "first"},
            "restart_required": True,
        })
        self.assertFalse(self.persisted(first).enabled)
        self.assertEqual(self.persisted(first).options, {"message": "first"})
        self.assert_status(self.enable(first), 200, restart=False)
        self.assertIs(self.app.state.handler, handler)
        self.assert_chat("first")
        self.assert_status(
            self.request("PATCH", "/" + first, json={"enabled": False}), 200, restart=True,
        )
        self.restart()
        self.assert_chat("fallback")
        self.assert_status(self.request("GET"), 200, restart=False)

    def test_disabled_settings_are_replaced_and_validation_waits_for_startup(self):
        name = self.name()
        self.assert_status(self.upload(name, options={"message": "old"}), 201, restart=False)
        options = {"unknown_argument": "cannot build this factory"}
        response = self.request("PUT", "/" + name, json={"options": options})
        self.assert_status(response, 200, restart=False)
        self.assertEqual(response.json(), {"name": name, "enabled": False, "options": options})
        self.assertEqual(self.persisted(name).options, options)
        self.restart()
        self.assert_status(self.enable(name), 200, restart=True)
        self.assertEqual(self.item(name), {
            "name": name, "enabled": False, "options": {}, "restart_required": True,
        })
        self.assertEqual(self.persisted(name).options, options)
        self.assertTrue(self.persisted(name).enabled)
        saved = self.config_path.read_bytes()
        with self.assertRaisesRegex(TypeError, "unknown_argument"):
            self.restart()
        self.assertEqual(self.config_path.read_bytes(), saved)

    def test_invalid_active_settings_are_saved_but_fail_at_startup(self):
        name = self.name()
        self.assert_status(self.upload(name, options={"message": "original"}), 201)
        self.enable(name)
        self.restart()
        handler = self.app.state.handler
        for options in [{"unknown_argument": True}, {"message": 123}]:
            with self.subTest(options=options):
                response = self.request("PUT", "/" + name, json={"options": options})
                self.assert_status(response, 200, restart=True)
                self.assertEqual(response.json(), {"name": name, "enabled": True, "options": options})
                self.assertEqual(self.persisted(name).options, options)
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat("original")
        before = self.config_path.read_bytes()
        expected = self.item(name)
        self.assert_status(self.request("PUT", "/" + name, json={"options": {"message": None}}), 422)
        self.assertEqual(self.config_path.read_bytes(), before)
        self.assertEqual(self.item(name), expected)
        self.assertIs(self.app.state.handler, handler)
        self.assert_chat("original")
        with self.assertRaisesRegex(ValueError, "message must be a string"):
            self.restart()
        self.assertEqual(self.config_path.read_bytes(), before)

    def test_invalid_factories_are_enabled_without_import_and_fail_at_startup(self):
        active = self.name()
        self.assert_status(self.upload(active, options={"message": "stable"}), 201)
        self.enable(active)
        self.restart()
        sources = [
            ("VALUE = 1\n", AttributeError),
            ("factory = 42\n", TypeError),
            ('raise RuntimeError("import failed")\n' + PLUGIN_SOURCE, RuntimeError),
            ('def factory(next_handler):\n    raise ValueError("invalid factory")\n', ValueError),
            ("def factory(next_handler):\n    return None\n", TypeError),
        ]
        for source, error in sources:
            with self.subTest(source=source):
                name = self.name()
                self.assert_status(self.upload(name, source), 201, restart=False)
                handler = self.app.state.handler
                response = self.enable(name)
                self.assert_status(response, 200, restart=True)
                self.assertEqual(self.item(name), {
                    "name": name, "enabled": False, "options": {}, "restart_required": True,
                })
                self.assertTrue(self.persisted(name).enabled)
                self.assertNotIn(f"gateway.plugins.{name}", sys.modules)
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat("stable")
                saved = self.config_path.read_bytes()
                with self.assertRaises(error):
                    self.restart()
                self.assertEqual(self.config_path.read_bytes(), saved)
                # Restore a bootable config outside HTTP, since startup failed.
                self.app.state.plugin_manager.update(name, enabled=False)
                self.restart()
                self.assert_chat("stable")

    def test_admin_bearer_is_required_for_every_route(self):
        name = self.name()
        self.assert_status(self.upload(name), 201)
        before = self.config_path.read_bytes()
        routes = [
            ("GET", "", {}),
            ("GET", "/" + name, {}),
            ("POST", "", {"files": {"file": (self.name() + ".py", PLUGIN_SOURCE)}}),
            ("PUT", "/" + name, {"json": {"options": {}}}),
            ("PATCH", "/" + name, {"json": {"enabled": True}}),
            ("DELETE", "/" + name, {}),
        ]
        for headers in [{}, {"Authorization": "Bearer wrong"}, GATEWAY_HEADERS,
                        {"Authorization": "Basic test-admin-key"}]:
            for method, path, kwargs in routes:
                with self.subTest(headers=headers, method=method):
                    self.assert_status(self.request(method, path, headers=headers, **kwargs), 401)
                    self.assertEqual(self.config_path.read_bytes(), before)
        self.assertEqual(self.item(name)["enabled"], False)
        self.assertFalse(self.persisted(name).enabled)
        self.assertEqual(len(list(self.plugin_directory.glob("*.py"))), 1)

    def test_missing_admin_key_fails_closed_even_with_gateway_key(self):
        for value in [None, ""]:
            with self.subTest(admin_key=value), patch.dict(os.environ):
                if value is None:
                    os.environ.pop("GATEWAY_ADMIN_API_KEY", None)
                else:
                    os.environ["GATEWAY_ADMIN_API_KEY"] = value
                self.restart()
                for headers in [{}, ADMIN_HEADERS, GATEWAY_HEADERS]:
                    self.assert_status(self.request("GET", headers=headers), 503)

    def test_disabling_gateway_authentication_does_not_disable_admin_auth(self):
        authentication = self.name()
        package_file = gateway.plugins.__file__
        assert package_file is not None
        source = (Path(package_file).parent / "authentication.py").read_text()
        self.assert_status(self.upload(authentication, source), 201)
        self.enable(authentication)
        responder = self.name()
        self.assert_status(self.upload(responder), 201)
        self.enable(responder)
        self.restart()
        for headers in [{}, ADMIN_HEADERS]:
            response = self.client.post("/v1/chat/completions", json=CHAT_BODY, headers=headers)
            self.assert_status(response, 401)
        self.assert_chat("default", headers=GATEWAY_HEADERS)
        self.assert_status(
            self.request("PATCH", "/" + authentication, json={"enabled": False}), 200, restart=True,
        )
        self.assert_status(self.client.post("/v1/chat/completions", json=CHAT_BODY), 401)
        self.assert_chat("default", headers=GATEWAY_HEADERS)
        self.restart()
        self.assert_chat("default")
        self.assert_status(self.request("GET", headers={}), 401)
        self.assert_status(self.request("GET", headers=GATEWAY_HEADERS), 401)
        self.assert_status(self.request("GET"), 200)

    def test_duplicate_upload_preserves_original_file_and_config(self):
        name = self.name()
        self.assert_status(self.upload(name, options={"message": "original"}), 201)
        before = self.config_path.read_bytes()
        response = self.upload(name, "factory = None\n", {"message": "replacement"})
        self.assert_status(response, 409)
        self.assertEqual((self.plugin_directory / (name + ".py")).read_text(), PLUGIN_SOURCE)
        self.assertEqual(self.config_path.read_bytes(), before)
        self.assertEqual(self.persisted(name).options, {"message": "original"})
        self.assertEqual(self.item(name), {
            "name": name, "enabled": False, "options": {}, "restart_required": False,
        })

    def test_missing_plugin_returns_404(self):
        path = "/" + self.name()
        before = self.config_path.read_bytes()
        for method, kwargs in [
            ("GET", {}), ("PUT", {"json": {"options": {}}}),
            ("PATCH", {"json": {"enabled": True}}), ("DELETE", {}),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.request(method, path, **kwargs), 404)
                self.assertEqual(self.config_path.read_bytes(), before)

    def test_bad_upload_filenames_and_traversal_are_rejected(self):
        before = self.config_path.read_bytes()
        for filename in ["../escape.py", "nested/escape.py", "/escape.py", "bad-name.py", "plugin.txt", ".py"]:
            with self.subTest(filename=filename):
                response = self.request("POST", files={"file": (filename, PLUGIN_SOURCE)})
                self.assert_status(response, 400)
                self.assertEqual(self.config_path.read_bytes(), before)
                self.assertEqual(list(self.plugin_directory.iterdir()), [])
                self.assertFalse((self.root / "escape.py").exists())

    def test_bad_route_names_and_encoded_traversal_are_rejected(self):
        before = self.config_path.read_bytes()
        for name in ["bad-name", "bad.py", "%2E%2E", "%2E%2E%5Cescape"]:
            for method, kwargs in [
                ("GET", {}), ("PUT", {"json": {"options": {}}}),
                ("PATCH", {"json": {"enabled": True}}), ("DELETE", {}),
            ]:
                with self.subTest(name=name, method=method):
                    self.assert_status(self.request(method, "/" + name, **kwargs), 400)
                    self.assertEqual(self.config_path.read_bytes(), before)

    def test_malformed_source_leaves_no_file_or_config_entry(self):
        before = self.config_path.read_bytes()
        for source in ["def factory(:\n", b"\xff\xfe\x00"]:
            with self.subTest(source=source):
                name = self.name()
                self.assert_status(self.upload(name, source), 400)
                self.assertFalse((self.plugin_directory / (name + ".py")).exists())
                self.assertEqual(self.config_path.read_bytes(), before)
                self.assert_status(self.request("GET", "/" + name), 404)

    def test_upload_size_limit_is_one_mebibyte(self):
        limit = 1024 * 1024
        source = PLUGIN_SOURCE.encode() + b"\n#"
        source += b"x" * (limit - len(source))
        accepted = self.name()
        self.assert_status(self.upload(accepted, source), 201)
        self.assertEqual((self.plugin_directory / (accepted + ".py")).stat().st_size, limit)
        before = self.config_path.read_bytes()
        rejected = self.name()
        self.assert_status(self.upload(rejected, source + b"x"), 413)
        self.assertFalse((self.plugin_directory / (rejected + ".py")).exists())
        self.assertEqual(self.config_path.read_bytes(), before)

    def test_upload_options_must_be_a_json_object(self):
        before = self.config_path.read_bytes()
        for options in ["{", "[]", '"text"', "42", "true", "null"]:
            with self.subTest(options=options):
                name = self.name()
                response = self.request(
                    "POST", files={"file": (name + ".py", PLUGIN_SOURCE)},
                    data={"options": options},
                )
                self.assertIn(response.status_code, (400, 422), response.text)
                self.assertFalse((self.plugin_directory / (name + ".py")).exists())
                self.assertEqual(self.config_path.read_bytes(), before)
        response = self.request("POST", data={"options": "{}"})
        self.assertIn(response.status_code, (400, 422), response.text)
        self.assertEqual(self.config_path.read_bytes(), before)

    def test_null_options_are_not_toml_serializable_even_when_disabled(self):
        name = self.name()
        self.assert_status(self.upload(name), 201)
        before = self.config_path.read_bytes()
        for options in [
            {"message": None}, {"metadata": {"nested": None}},
            {"metadata": {"values": ["ok", None]}},
        ]:
            with self.subTest(options=options):
                rejected = self.name()
                self.assert_status(self.upload(rejected, options=options), 422)
                self.assertFalse((self.plugin_directory / (rejected + ".py")).exists())
                self.assert_status(self.request("PUT", "/" + name, json={"options": options}), 422)
                self.assertEqual(self.config_path.read_bytes(), before)
                self.assertEqual(self.item(name), {
                    "name": name, "enabled": False, "options": {}, "restart_required": False,
                })
                self.assertEqual(self.persisted(name).options, {})

    def test_invalid_json_and_request_bodies_leave_state_unchanged(self):
        name = self.name()
        self.assert_status(self.upload(name), 201)
        before = self.config_path.read_bytes()
        for method, bodies in [
            ("PUT", [{}, [], {"options": []}, {"options": "text"}, {"options": None}]),
            ("PATCH", [{}, [], {"enabled": "false"}, {"enabled": 1}, {"enabled": None}]),
        ]:
            for body in bodies:
                with self.subTest(method=method, body=body):
                    response = self.request(method, "/" + name, json=body)
                    self.assertIn(response.status_code, (400, 422), response.text)
                    self.assertEqual(self.config_path.read_bytes(), before)
                    self.assertEqual(self.item(name), {
                        "name": name, "enabled": False, "options": {}, "restart_required": False,
                    })
                    self.assertFalse(self.persisted(name).enabled)
                    self.assertEqual(self.persisted(name).options, {})
            response = self.request(
                method, "/" + name, content="{",
                headers={**ADMIN_HEADERS, "Content-Type": "application/json"},
            )
            self.assertIn(response.status_code, (400, 422), response.text)
            self.assertEqual(self.config_path.read_bytes(), before)

    def ordering_request(self, method, name, body):
        if method == "POST":
            data = dict(body)
            if "options" in data:
                data["options"] = json.dumps(data["options"])
            return self.request(
                "POST", files={"file": (name + ".py", PLUGIN_SOURCE)}, data=data,
            )
        return self.request(method, "/" + name, json=body)

    def assert_plugin_order(self, names):
        factories = [f"gateway.plugins.{name}:factory" for name in names]
        self.assertEqual([item.factory for item in load().plugins], factories)
        self.assertEqual(
            [item.factory for item in self.app.state.plugin_manager.config.plugins], factories,
        )

    def ordering_state(self):
        manager = self.app.state.plugin_manager
        return (
            self.config_path.read_bytes(),
            manager.config.model_copy(deep=True),
            manager.source, manager.restart_required,
            [item.model_copy(deep=True) for item in manager.startup_plugins],
            {path.relative_to(self.plugin_directory): path.read_bytes()
             for path in self.plugin_directory.rglob("*") if path.is_file()},
        )

    def test_ordered_uploads_insert_next_to_disabled_targets_and_default_to_append(self):
        first, disabled, last = [self.prefix + suffix for suffix in ("_z", "_m", "_a")]
        for name in (first, disabled, last):
            self.assert_status(self.upload(name, options={"message": name}), 201)
        self.enable(first)
        self.enable(last)
        self.restart()
        handler = self.app.state.handler
        appended, beginning, middle, ending = [self.name() for _ in range(4)]
        for name, body, expected in [
            (appended, {}, [first, disabled, last, appended]),
            (beginning, {"before": first}, [beginning, first, disabled, last, appended]),
            (middle, {"after": disabled}, [beginning, first, disabled, middle, last, appended]),
            (ending, {"after": appended}, [beginning, first, disabled, middle, last, appended, ending]),
        ]:
            with self.subTest(body=body):
                response = self.ordering_request("POST", name, {"options": {"message": name}, **body})
                self.assert_status(response, 201, restart=False)
                self.assertEqual(response.json(), {
                    "name": name, "enabled": False, "options": {"message": name},
                })
                self.assert_plugin_order(expected)
                self.assertEqual((self.plugin_directory / (name + ".py")).read_text(), PLUGIN_SOURCE)
                saved = self.ordering_state()
                self.assert_status(self.ordering_request("POST", name, body), 409)
                self.assertEqual(self.ordering_state(), saved)
                listing = self.request("GET")
                self.assert_status(listing, 200, restart=False)
                self.assertEqual([item["name"] for item in listing.json()], sorted(expected))
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat(first)
        self.restart()
        self.assert_plugin_order(expected)
        self.assert_chat(first)

    def test_put_and_order_only_patch_move_both_directions_and_retry_idempotently(self):
        first, second, third, last = [self.name() for _ in range(4)]
        for name in (first, second, third, last):
            self.assert_status(self.upload(name), 201)
        for method in ("PUT", "PATCH"):
            for name, hint, expected in [
                (first, {"after": third}, [second, third, first, last]),
                (first, {"before": second}, [first, second, third, last]),
                (last, {"before": second}, [first, last, second, third]),
                (last, {"after": third}, [first, second, third, last]),
            ]:
                with self.subTest(method=method, name=name, hint=hint):
                    body = {"options": {}, **hint} if method == "PUT" else hint
                    self.assert_status(self.ordering_request(method, name, body), 200, restart=False)
                    self.assert_plugin_order(expected)
                    self.assertFalse(self.persisted(name).enabled)
                    saved = self.ordering_state()
                    self.assert_status(self.ordering_request(method, name, body), 200, restart=False)
                    self.assertEqual(self.ordering_state(), saved)
                    self.assert_plugin_order(expected)
        self.restart()
        self.assert_plugin_order([first, second, third, last])

    def test_updates_without_hints_or_with_null_hints_preserve_existing_position(self):
        first, middle, last = [self.name() for _ in range(3)]
        for name in (first, middle, last):
            self.assert_status(self.upload(name), 201)
        for method, body in [
            ("PUT", {"options": {"message": "updated"}}),
            ("PATCH", {"enabled": True}),
            ("PUT", {"options": {}, "before": None}),
            ("PUT", {"options": {}, "after": None}),
            ("PUT", {"options": {}, "before": None, "after": None}),
            ("PATCH", {"enabled": False, "before": None}),
            ("PATCH", {"enabled": False, "after": None}),
            ("PATCH", {"enabled": False, "before": None, "after": None}),
        ]:
            with self.subTest(method=method, body=body):
                self.assert_status(self.ordering_request(method, middle, body), 200)
                self.assert_plugin_order([first, middle, last])
        for method in ("PUT", "PATCH"):
            body = {"options": {}} if method == "PUT" else {}
            for hint, expected in [
                ({"before": None, "after": last}, [first, last, middle]),
                ({"before": first, "after": None}, [middle, first, last]),
                ({"before": None, "after": first}, [first, middle, last]),
            ]:
                with self.subTest(method=method, hint=hint):
                    self.assert_status(self.ordering_request(method, middle, {**body, **hint}), 200, restart=False)
                    self.assert_plugin_order(expected)

    def test_source_only_movers_are_configured_disabled_at_requested_position(self):
        first, last = self.name(), self.name()
        for name in (first, last):
            self.assert_status(self.upload(name), 201)
        source = 'raise RuntimeError("moving source-only plugins must not import them")\n'
        for method in ("PUT", "PATCH"):
            for hint in ({}, {"before": first}, {"after": first}):
                with self.subTest(method=method, hint=hint):
                    name = self.name()
                    path = self.plugin_directory / (name + ".py")
                    path.write_text(source, encoding="utf-8")
                    body = ({"options": {"message": "saved"}} if method == "PUT" else
                            {} if hint else {"enabled": False})
                    try:
                        response = self.ordering_request(method, name, {**body, **hint})
                        self.assert_status(response, 200, restart=False)
                        self.assertEqual(response.json(), {
                            "name": name, "enabled": False, "options": body.get("options", {}),
                        })
                        expected = ([name, first, last] if "before" in hint else
                                    [first, name, last] if "after" in hint else [first, last, name])
                        self.assert_plugin_order(expected)
                        saved = self.ordering_state()
                        self.assert_status(self.ordering_request(method, name, {**body, **hint}), 200, restart=False)
                        self.assertEqual(self.ordering_state(), saved)
                        self.restart()
                        self.assert_plugin_order(expected)
                        self.assertNotIn(f"gateway.plugins.{name}", sys.modules)
                        self.assertEqual(path.read_text(), source)
                    finally:
                        self.assert_status(self.request("DELETE", "/" + name), 204)
                    self.assert_plugin_order([first, last])

    def test_ordering_request_validation_does_not_change_files_or_state(self):
        name, target = self.name(), self.name()
        for plugin in (name, target):
            self.assert_status(self.upload(plugin), 201)
        saved = self.ordering_state()
        bodies = [
            ("PUT", {"before": target}),
            ("PUT", {"after": target}),
            ("PUT", {"options": None, "before": target}),
            ("PATCH", {}),
            ("PATCH", {"before": None}),
            ("PATCH", {"after": None}),
            ("PATCH", {"before": None, "after": None}),
            ("PATCH", {"enabled": None}),
            ("PATCH", {"enabled": None, "before": target}),
            ("PATCH", {"enabled": None, "after": target}),
            ("PATCH", {"enabled": "false", "before": target}),
            ("PATCH", {"enabled": 1, "after": target}),
        ]
        for method in ("PUT", "PATCH"):
            for hint in ("before", "after"):
                for value in (False, 1, [], {}):
                    body = {"options": {}} if method == "PUT" else {"enabled": True}
                    bodies.append((method, {**body, hint: value}))
        for method, body in bodies:
            with self.subTest(method=method, body=body):
                response = self.ordering_request(method, name, body)
                self.assertIn(response.status_code, (400, 422), response.text)
                self.assertEqual(self.ordering_state(), saved)

    def test_invalid_order_targets_are_atomic_and_failed_uploads_can_be_retried(self):
        active, moving, target, source_only, missing, uploaded = [self.name() for _ in range(6)]
        for name in (active, moving, target):
            self.assert_status(self.upload(name, options={"message": name}), 201)
        self.enable(active)
        (self.plugin_directory / (source_only + ".py")).write_text(PLUGIN_SOURCE, encoding="utf-8")
        self.restart()
        handler = self.app.state.handler
        for method in ("POST", "PUT", "PATCH"):
            name = uploaded if method == "POST" else moving
            changes = {"enabled": True} if method == "PATCH" else {"options": {"message": "changed"}}
            cases = [({"before": target, "after": active}, 400)]
            for hint in ("before", "after"):
                cases.extend(({hint: value}, status) for value, status in [
                    (name, 400), ("bad-name", 400), ("../escape", 400),
                    ("plugin.py", 400), ("__init__", 400), ("class", 400),
                    (missing, 404), (source_only, 404),
                ])
                if method != "POST":
                    cases.append(({hint: ""}, 400))
            for hint, status in cases:
                with self.subTest(method=method, hint=hint):
                    saved = self.ordering_state()
                    listing = self.request("GET").json()
                    self.assert_status(self.ordering_request(method, name, {**changes, **hint}), status)
                    self.assertEqual(self.ordering_state(), saved)
                    self.assertEqual(self.request("GET").json(), listing)
                    self.assertIs(self.app.state.handler, handler)
                    self.assert_chat(active)
                    self.assertFalse((self.plugin_directory / (uploaded + ".py")).exists())
        self.assert_status(self.ordering_request("POST", uploaded, {"before": target}), 201, restart=False)
        self.assert_plugin_order([active, moving, uploaded, target])
        self.assertEqual((self.plugin_directory / (uploaded + ".py")).read_text(), PLUGIN_SOURCE)

    def test_ambiguous_order_targets_return_conflict_without_mutation(self):
        moving, target, uploaded = self.name(), self.name(), self.name()
        for name in (moving, target):
            self.assert_status(self.upload(name), 201)
        with self.config_path.open("a", encoding="utf-8") as config:
            config.write(
                f'\n[[plugins]]\nfactory = "gateway.plugins.{target}:other_factory"\n'
                'enabled = false\n'
            )
        self.restart()
        saved = self.ordering_state()
        for method in ("POST", "PUT", "PATCH"):
            name = uploaded if method == "POST" else moving
            changes = {"enabled": True} if method == "PATCH" else {"options": {"message": "changed"}}
            for hint in ("before", "after"):
                with self.subTest(method=method, hint=hint):
                    self.assert_status(self.ordering_request(method, name, {**changes, hint: target}), 409)
                    self.assertEqual(self.ordering_state(), saved)
                    self.assertEqual(self.item(moving), {
                        "name": moving, "enabled": False, "options": {}, "restart_required": False,
                    })
                    self.assertFalse(self.persisted(moving).enabled)
                    self.assertEqual(self.persisted(moving).options, {})
                    self.assertFalse((self.plugin_directory / (uploaded + ".py")).exists())

    def test_ordered_mutations_roll_back_options_status_and_source_on_save_failure(self):
        active, moving, target, source_only, uploaded = [self.name() for _ in range(5)]
        for name in (active, moving, target):
            self.assert_status(self.upload(name, options={"message": name}), 201)
        self.enable(active)
        (self.plugin_directory / (source_only + ".py")).write_text(PLUGIN_SOURCE, encoding="utf-8")
        self.restart()
        handler = self.app.state.handler
        saved = self.ordering_state()
        running = self.request("GET").json()
        for method, name, body in [
            ("POST", uploaded, {"options": {"message": "new"}, "before": active}),
            ("PUT", moving, {"options": {"message": "changed"}, "after": target}),
            ("PUT", source_only, {"options": {"message": "changed"}, "before": active}),
            ("PATCH", moving, {"enabled": True, "before": active}),
            ("PATCH", active, {"enabled": False, "after": target}),
            ("PATCH", source_only, {"enabled": True, "before": active}),
            ("PATCH", moving, {"after": target}),
        ]:
            with self.subTest(method=method, name=name, body=body):
                with self.fail_config_replace():
                    self.assert_status(self.ordering_request(method, name, body), 500)
                self.assertEqual(self.ordering_state(), saved)
                self.assert_running(running, restart=False)
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat(active)
                self.assertFalse((self.plugin_directory / (uploaded + ".py")).exists())
        self.assert_status(self.ordering_request("POST", uploaded, {"before": active}), 201, restart=False)
        self.assert_plugin_order([uploaded, active, moving, target])
        self.assert_status(self.ordering_request("PUT", moving, {
            "options": {"message": "changed"}, "after": target,
        }), 200, restart=False)
        self.assert_plugin_order([uploaded, active, target, moving])
        self.assertEqual(self.persisted(moving).options, {"message": "changed"})
        self.assert_status(self.ordering_request("PATCH", moving, {
            "enabled": True, "before": active,
        }), 200, restart=True)
        self.assert_plugin_order([uploaded, moving, active, target])
        self.assert_chat(active)
        self.restart()
        self.assert_chat("changed")

    def test_invalid_options_with_valid_order_hint_do_not_move_or_upload(self):
        first, moving, uploaded = self.name(), self.name(), self.name()
        for name in (first, moving):
            self.assert_status(self.upload(name), 201)
        saved = self.ordering_state()
        for method, name in (("POST", uploaded), ("PUT", moving)):
            with self.subTest(method=method):
                self.assert_status(self.ordering_request(method, name, {
                    "options": {"metadata": {"nested": None}}, "before": first,
                }), 422)
                self.assertEqual(self.ordering_state(), saved)
                self.assert_plugin_order([first, moving])
                self.assertFalse((self.plugin_directory / (uploaded + ".py")).exists())

    def test_ordered_mutations_reject_external_config_edits_atomically(self):
        first, moving, uploaded = self.name(), self.name(), self.name()
        for name in (first, moving):
            self.assert_status(self.upload(name), 201)
        self.config_path.write_bytes(self.config_path.read_bytes() + b"\n# External order edit\n")
        saved = self.ordering_state()
        for method, name, body in [
            ("POST", uploaded, {"before": first}),
            ("PUT", moving, {"options": {"message": "changed"}, "before": first}),
            ("PATCH", moving, {"enabled": True, "before": first}),
            ("PATCH", moving, {"after": first}),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.ordering_request(method, name, body), 409)
                self.assertEqual(self.ordering_state(), saved)
                self.assertFalse((self.plugin_directory / (uploaded + ".py")).exists())

    def test_saved_order_only_changes_runtime_after_restart_and_get_stays_alphabetical(self):
        first, disabled, second, terminal = [
            self.prefix + suffix for suffix in ("_z", "_m", "_a", "_terminal")
        ]
        source = '''def factory(next_handler, *, message):
    async def handle(context):
        response = await next_handler(context)
        response.body["metadata"].setdefault("order", []).insert(0, message)
        return response
    return handle
'''
        for name in (first, disabled, second):
            self.assert_status(self.upload(name, source, {"message": name}), 201)
        self.assert_status(self.upload(terminal, options={"message": "terminal"}), 201)
        for name in (first, second, terminal):
            self.enable(name)
        self.restart()
        handler = self.app.state.handler
        self.assert_chat("terminal", {"order": [first, second]})
        for body, expected, pending in [
            ({"after": second}, [disabled, second, first, terminal], True),
            ({"before": disabled}, [first, disabled, second, terminal], False),
            ({"after": second}, [disabled, second, first, terminal], True),
        ]:
            with self.subTest(body=body, pending=pending):
                self.assert_status(self.ordering_request("PATCH", first, body), 200, restart=pending)
                self.assert_plugin_order(expected)
                saved = self.ordering_state()
                self.assert_status(self.ordering_request("PATCH", first, body), 200, restart=pending)
                self.assertEqual(self.ordering_state(), saved)
                self.assertIs(self.app.state.handler, handler)
                self.assert_chat("terminal", {"order": [first, second]})
                listing = self.request("GET")
                self.assert_status(listing, 200, restart=pending)
                self.assertEqual([item["name"] for item in listing.json()], sorted(expected))
                self.assert_status(self.request("GET", "/" + first), 200, restart=pending)
        self.restart()
        self.assert_plugin_order([disabled, second, first, terminal])
        self.assert_chat("terminal", {"order": [second, first]})
        self.assert_status(self.request("GET"), 200, restart=False)
        self.assert_status(self.ordering_request("PATCH", disabled, {"after": first}), 200, restart=False)
        self.assert_plugin_order([second, first, disabled, terminal])
        self.assert_chat("terminal", {"order": [second, first]})
        self.assert_status(self.ordering_request("PATCH", disabled, {
            "enabled": True, "before": first,
        }), 200, restart=True)
        self.assert_plugin_order([second, disabled, first, terminal])
        self.assert_chat("terminal", {"order": [second, first]})
        self.restart()
        self.assert_chat("terminal", {"order": [second, disabled, first]})
        self.assert_status(self.ordering_request("PUT", first, {
            "options": {"message": "updated"}, "before": second,
        }), 200, restart=True)
        self.assert_plugin_order([first, second, disabled, terminal])
        self.assertEqual(self.persisted(first).options, {"message": "updated"})
        self.assertEqual(self.item(first)["options"], {"message": first})
        self.assertTrue(self.item(first)["restart_required"])
        self.assert_chat("terminal", {"order": [second, disabled, first]})
        self.restart()
        self.assert_chat("terminal", {"order": ["updated", second, disabled]})
        self.assert_status(self.request("GET"), 200, restart=False)

    def test_reordering_preserves_upstream_and_comments_on_moved_plugin_tables(self):
        first, disabled, last, uploaded = [self.name() for _ in range(4)]
        upstream = (
            '# Operator configuration\n'
            '[upstream] # Keep upstream\n'
            'base_url = "http://127.0.0.1:1/v1"\n'
            'api_key_env = "TEST_UPSTREAM_API_KEY"\n'
            'timeout_seconds = 1.25 # Keep timeout\n\n'
        )
        tables = []
        for name in (first, disabled, last):
            tables.append(
                f'[[plugins]] # Table for {name}\n'
                f'factory = "gateway.plugins.{name}:factory" # Factory for {name}\n'
                'enabled = false\n'
                f'[plugins.options] # Options for {name}\n'
                f'message = "{name}" # Message for {name}\n\n'
            )
            (self.plugin_directory / (name + ".py")).write_text(PLUGIN_SOURCE, encoding="utf-8")
        self.config_path.write_text(upstream + "".join(tables), encoding="utf-8")
        self.restart()
        expected_upstream = load().upstream
        for method, name, body, expected in [
            ("POST", uploaded, {"before": disabled}, [first, uploaded, disabled, last]),
            ("PUT", last, {"options": {"message": last}, "before": first}, [last, first, uploaded, disabled]),
            ("PATCH", first, {"after": disabled}, [last, uploaded, disabled, first]),
        ]:
            with self.subTest(method=method):
                self.assert_status(self.ordering_request(method, name, body), 201 if method == "POST" else 200, restart=False)
                self.assert_plugin_order(expected)
                text = self.config_path.read_text()
                self.assertTrue(text.startswith(upstream), text)
                self.assertEqual(load().upstream, expected_upstream)
                for original in (first, disabled, last):
                    for label in ("Table", "Factory", "Options", "Message"):
                        self.assertIn(f"# {label} for {original}", text)
                    self.assertEqual(self.persisted(original).options, {"message": original})
        self.restart()
        self.assert_plugin_order([last, uploaded, disabled, first])


if __name__ == "__main__":
    unittest.main()
