import asyncio
import importlib
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, Mock, patch

import httpx
from fastapi.testclient import TestClient
from openai import APIConnectionError, APIStatusError, APITimeoutError
from starlette.datastructures import State

from gateway import schema

CHAT_PATH = "/v1/chat/completions"
CHAT_BODY = {
    "model": "test-model",
    "messages": [{"role": "user", "content": "hello"}],
}
GATEWAY_HEADERS = {"Authorization": "Bearer test-gateway-key"}
ERROR_REF = "#/components/schemas/ErrorResponse"
OPERATIONS = (
    ("/admin/plugins", "get", 200, {400, 401, 409, 500, 503}),
    ("/admin/plugins", "post", 201, {400, 401, 404, 409, 413, 422, 500, 503}),
    ("/admin/plugins/{name}", "get", 200, {400, 401, 404, 409, 500, 503}),
    ("/admin/plugins/{name}", "put", 200, {400, 401, 404, 409, 422, 500, 503}),
    ("/admin/plugins/{name}", "patch", 200, {400, 401, 404, 409, 500, 503}),
    ("/admin/plugins/{name}", "delete", 204, {400, 401, 404, 409, 500, 503}),
    (CHAT_PATH, "post", 200, {400, 401, 403, 429, 500, 502, 504}),
)


class OpenAPIErrorTests(unittest.TestCase):
    def setUp(self):
        self.app = importlib.import_module("gateway.app").app
        self.enterContext(patch.object(self.app, "openapi_schema", None))
        self.document = self.app.openapi()

    def test_operation_status_sets_and_error_headers(self):
        for path, method, success, errors in OPERATIONS:
            with self.subTest(path=path, method=method):
                responses = self.document["paths"][path][method]["responses"]
                self.assertEqual(set(responses), {str(status) for status in errors | {success}})
                for status in errors:
                    with self.subTest(status=status):
                        response = responses[str(status)]
                        self.assertEqual(
                            response["content"]["application/json"]["schema"],
                            {"$ref": ERROR_REF},
                        )
                        headers = {name.lower(): value for name, value in response["headers"].items()}
                        self.assertEqual(headers["x-request-id"]["schema"]["type"], "string")
                        if status == 401:
                            challenge = headers["www-authenticate"]
                            self.assertEqual(challenge["schema"]["type"], "string")
                            self.assertIn("Bearer", json.dumps(challenge))
                if path.startswith("/admin/"):
                    headers = {name.lower() for name in responses[str(success)]["headers"]}
                    self.assertIn("x-restart-required", headers)

    def test_plugin_get_schemas_describe_runtime_without_changing_mutation_bodies(self):
        schemas = self.document["components"]["schemas"]
        fields = {"name": "string", "enabled": "boolean", "options": "object"}
        for path, method, success, _ in OPERATIONS:
            if not path.startswith("/admin/") or method == "delete":
                continue
            with self.subTest(path=path, method=method):
                response = self.document["paths"][path][method]["responses"][str(success)]
                body = response["content"]["application/json"]["schema"]
                if method == "get" and path == "/admin/plugins":
                    self.assertEqual(body["type"], "array")
                    body = body["items"]
                if "$ref" in body:
                    body = schemas[body["$ref"].removeprefix("#/components/schemas/")]
                expected = {**fields, "restart_required": "boolean"} if method == "get" else fields
                self.assertEqual(set(body["properties"]), set(expected))
                self.assertEqual(set(body["required"]), set(expected))
                for name, kind in expected.items():
                    self.assertEqual(body["properties"][name]["type"], kind)
                header = next(value for name, value in response["headers"].items()
                              if name.lower() == "x-restart-required")
                self.assertEqual(header["schema"], {"type": "string", "enum": ["true", "false"]})

    def test_error_response_schema_matches_gateway_envelope(self):
        schemas = self.document["components"]["schemas"]
        envelope = schemas["ErrorResponse"]
        self.assertEqual(envelope["type"], "object")
        self.assertEqual(set(envelope["properties"]), {"error"})
        self.assertIn("error", envelope["required"])
        detail = envelope["properties"]["error"]
        if "$ref" in detail:
            detail = schemas[detail["$ref"].removeprefix("#/components/schemas/")]
        self.assertEqual(detail["type"], "object")
        fields = detail["properties"]
        self.assertEqual(set(fields), {"message", "type", "param", "code"})
        self.assertEqual(fields["message"]["type"], "string")
        self.assertEqual(fields["code"]["type"], "string")
        self.assertEqual(fields["type"]["type"], "string")
        self.assertEqual(fields["type"]["const"], "gateway_error")
        self.assertEqual(fields["param"]["type"], "null")

    def test_no_automatic_validation_errors_and_only_genuine_422_responses(self):
        schemas = self.document["components"]["schemas"]
        self.assertNotIn("HTTPValidationError", schemas)
        self.assertNotIn("ValidationError", schemas)
        self.assertNotIn("#/components/schemas/HTTPValidationError", json.dumps(self.document))
        operations_with_422 = {
            (path, method)
            for path, item in self.document["paths"].items()
            for method, operation in item.items()
            if isinstance(operation, dict) and "422" in operation.get("responses", {})
        }
        self.assertEqual(operations_with_422, {
            ("/admin/plugins", "post"),
            ("/admin/plugins/{name}", "put"),
        })

    def test_health_contract_is_unchanged(self):
        responses = self.document["paths"]["/health"]["get"]["responses"]
        self.assertEqual(set(responses), {"200"})
        self.assertNotIn("headers", responses["200"])
        client = TestClient(self.app)
        self.addCleanup(client.close)
        response = client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})
        self.assertNotIn("X-Request-ID", response.headers)
        self.assertNotIn("X-Restart-Required", response.headers)


class ChatErrorTests(unittest.TestCase):
    def setUp(self):
        self.app = importlib.import_module("gateway.app").app
        root = Path(self.enterContext(tempfile.TemporaryDirectory()))
        config_path = root / "config.toml"
        config_path.write_text(
            '[upstream]\n'
            'base_url = "http://127.0.0.1:1/v1"\n'
            'api_key_env = "TEST_UPSTREAM_API_KEY"\n'
            'timeout_seconds = 0.05\n\n'
            '[[plugins]]\n'
            'factory = "gateway.plugins.authentication:factory"\n\n'
            '[[plugins]]\n'
            'factory = "gateway.plugins.model_allowlist:factory"\n'
            '[plugins.options]\n'
            'models = ["test-model"]\n',
            encoding="utf-8",
        )
        self.enterContext(patch.dict(os.environ, {
            "GATEWAY_CONFIG": str(config_path),
            "GATEWAY_API_KEY": "test-gateway-key",
            "GATEWAY_ADMIN_API_KEY": "test-admin-key",
            "TEST_UPSTREAM_API_KEY": "not-a-real-upstream-key",
        }))
        self.enterContext(patch.object(self.app, "state", State()))
        self.enterContext(patch.object(self.app, "openapi_schema", None))
        self.network_send = self.enterContext(patch.object(
            httpx.AsyncClient, "send", new_callable=AsyncMock,
            side_effect=AssertionError("Tests must not contact an upstream"),
        ))
        self.upstream = Mock()
        self.create = AsyncMock(return_value=Mock(
            model_dump=Mock(return_value={"id": "test-completion", "choices": []}),
        ))
        self.upstream.chat.completions.create = self.create
        self.openai = self.enterContext(patch("gateway.app.AsyncOpenAI"))
        self.openai.return_value.__aenter__.return_value = self.upstream
        self.client = self.enterContext(TestClient(self.app))
        self.upstream_request = httpx.Request("POST", "http://127.0.0.1:1/v1/chat/completions")

    def tearDown(self):
        self.network_send.assert_not_called()

    def post(self, **kwargs):
        kwargs.setdefault("headers", GATEWAY_HEADERS)
        if "content" not in kwargs:
            kwargs.setdefault("json", CHAT_BODY)
        return self.client.post(CHAT_PATH, **kwargs)

    def assert_error(self, response, status, code):
        self.assertEqual(response.status_code, status, response.text)
        self.assertEqual(response.headers["content-type"], "application/json")
        body = response.json()
        parsed = schema.ErrorResponse.model_validate(body, strict=True)
        self.assertEqual(parsed.model_dump(), body)
        self.assertEqual(set(body), {"error"})
        self.assertEqual(set(body["error"]), {"message", "type", "param", "code"})
        self.assertTrue(body["error"]["message"])
        self.assertEqual(body["error"]["type"], "gateway_error")
        self.assertIsNone(body["error"]["param"])
        self.assertEqual(body["error"]["code"], code)
        self.assertTrue(response.headers.get("X-Request-ID"))
        documented = self.app.openapi()["paths"][CHAT_PATH]["post"]["responses"][str(status)]
        self.assertEqual(documented["content"]["application/json"]["schema"], {"$ref": ERROR_REF})
        headers = {name.lower() for name in documented["headers"]}
        self.assertIn("x-request-id", headers)
        if status == 401:
            self.assertIn("www-authenticate", headers)
            self.assertEqual(response.headers.get("WWW-Authenticate"), "Bearer")
        else:
            self.assertNotIn("WWW-Authenticate", response.headers)
        self.assertNotIn("private-error-detail", response.text)

    def test_authentication_rejects_missing_wrong_and_admin_credentials(self):
        for authorization in (None, "Bearer wrong-key", "Basic test-gateway-key", "Bearer test-admin-key"):
            with self.subTest(authorization=authorization):
                headers = {} if authorization is None else {"Authorization": authorization}
                self.assert_error(self.post(headers=headers), 401, "unauthorized")
        self.create.assert_not_awaited()

    def test_allowlist_rejects_model_after_authentication(self):
        body = {**CHAT_BODY, "model": "blocked-model"}
        self.assert_error(self.post(json=body, headers={}), 401, "unauthorized")
        self.assert_error(self.post(json=body), 403, "model_not_allowed")
        self.create.assert_not_awaited()

    def test_valid_credentials_and_model_reach_mocked_upstream(self):
        response = self.post()
        self.assertEqual(response.status_code, 200, response.text)
        self.assertEqual(response.json(), {"id": "test-completion", "choices": []})
        self.create.assert_awaited_once_with(**CHAT_BODY, stream=False)
        self.openai.assert_called_once_with(
            base_url="http://127.0.0.1:1/v1", api_key="not-a-real-upstream-key",
            timeout=0.05, max_retries=0,
        )

    def test_upstream_timeout_exceptions_map_to_504(self):
        for exception in (TimeoutError("private-error-detail"), APITimeoutError(request=self.upstream_request)):
            with self.subTest(exception=type(exception).__name__):
                self.create.reset_mock()
                self.create.side_effect = exception
                self.assert_error(self.post(), 504, "upstream_timeout")
                self.create.assert_awaited_once()

    def test_gateway_deadline_cancels_upstream_and_maps_to_504(self):
        cancelled = []

        async def slow_completion(**kwargs):
            try:
                await asyncio.sleep(10)
            finally:
                cancelled.append(True)

        self.create.side_effect = slow_completion
        self.assert_error(self.post(), 504, "upstream_timeout")
        self.create.assert_awaited_once()
        self.assertEqual(cancelled, [True])

    def test_upstream_connection_error_maps_to_502(self):
        self.create.side_effect = APIConnectionError(
            message="private-error-detail", request=self.upstream_request,
        )
        self.assert_error(self.post(), 502, "upstream_unavailable")
        self.create.assert_awaited_once()

    def test_upstream_status_preserves_only_rate_limit_status(self):
        for upstream_status in (400, 401, 403, 404, 422, 429, 500, 502, 503, 504):
            with self.subTest(upstream_status=upstream_status):
                self.create.reset_mock()
                self.create.side_effect = APIStatusError(
                    "private-error-detail",
                    response=httpx.Response(upstream_status, request=self.upstream_request),
                    body={"error": "private-error-detail"},
                )
                self.assert_error(self.post(), 429 if upstream_status == 429 else 502, "upstream_error")
                self.create.assert_awaited_once()

    def test_unexpected_handler_exception_maps_to_500(self):
        handler = AsyncMock(side_effect=RuntimeError("private-error-detail"))
        with patch.object(self.app.state, "handler", handler):
            response = self.post()
        self.assert_error(response, 500, "gateway_error")
        handler.assert_awaited_once()
        self.assertEqual(response.headers["X-Request-ID"], handler.call_args.args[0].request_id)
        self.create.assert_not_awaited()

    def test_malformed_and_invalid_requests_return_400_not_422(self):
        requests = (
            {"content": '{"private-error-detail":', "headers": {
                **GATEWAY_HEADERS, "Content-Type": "application/json",
            }},
            {"content": b"", "headers": {**GATEWAY_HEADERS, "Content-Type": "application/json"}},
            {"json": {}},
            {"json": {**CHAT_BODY, "messages": []}},
            {"json": {**CHAT_BODY, "model": 123}},
            {"json": {**CHAT_BODY, "stream": True}},
            {"json": {**CHAT_BODY, "extra": "private-error-detail"}},
            {"json": {**CHAT_BODY, "max_tokens": 1, "max_completion_tokens": 1}},
        )
        request_ids = set()
        for kwargs in requests:
            with self.subTest(request=kwargs):
                response = self.post(**kwargs)
                request_ids.add(response.headers["X-Request-ID"])
                self.assert_error(response, 400, "invalid_request")
        self.assertEqual(len(request_ids), len(requests))
        self.create.assert_not_awaited()


if __name__ == "__main__":
    unittest.main()
