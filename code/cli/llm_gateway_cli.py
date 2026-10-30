
#!/usr/bin/env python3

import argparse
import json
import os
import sys
import uuid
import urllib.error
import urllib.parse
import urllib.request

from pathlib import Path


DEFAULT_HOST = "http://localhost:8080"


class GatewayClient:
    def __init__(self, host: str, api_key: str):
        self.host = host.rstrip("/")
        self.api_key = api_key

    def request(self, method, path, *, params=None, body=None, headers=None):
        url = self.host + path

        if params:
            query = urllib.parse.urlencode(params)
            url += "?" + query

        request_headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Accept": "application/json",
        }

        if headers:
            request_headers.update(headers)

        data = None

        if body is not None:
            if isinstance(body, (bytes, bytearray)):
                data = body
            else:
                data = json.dumps(body).encode("utf-8")
                request_headers["Content-Type"] = "application/json"

        req = urllib.request.Request(
            url,
            data=data,
            headers=request_headers,
            method=method,
        )

        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                response_data = response.read()

                if not response_data:
                    return None

                content_type = response.headers.get(
                    "Content-Type", ""
                )

                if "application/json" in content_type:
                    return json.loads(response_data)

                return response_data.decode("utf-8", errors="replace")

        except urllib.error.HTTPError as error:
            response_body = error.read().decode(
                "utf-8", errors="replace"
            )

            print(
                f"HTTP {error.code}: {error.reason}",
                file=sys.stderr,
            )

            if response_body:
                print(response_body, file=sys.stderr)

            sys.exit(1)

        except urllib.error.URLError as error:
            print(f"Connection error: {error.reason}", file=sys.stderr)
            sys.exit(1)

        except TimeoutError:
            print("Request timed out", file=sys.stderr)
            sys.exit(1)

    @staticmethod
    def plugin_path(name):
        return "/admin/plugins/" + urllib.parse.quote(name, safe="")

    def list_plugins(self):
        return self.request("GET", "/admin/plugins")

    def get_plugin(self, name):
        return self.request(
            "GET",
            self.plugin_path(name),
        )

    def update_plugin(self, name, options=None, before=None, after=None):
        body = {}

        if options is not None:
            body["options"] = options

        if before is not None:
            body["before"] = before

        if after is not None:
            body["after"] = after

        return self.request(
            "PUT",
            self.plugin_path(name),
            body=body,
        )

    def patch_plugin(self, name, enabled=None, before=None, after=None):
        body = {}

        if enabled is not None:
            body["enabled"] = enabled

        if before is not None:
            body["before"] = before

        if after is not None:
            body["after"] = after

        return self.request(
            "PATCH",
            self.plugin_path(name),
            body=body,
        )

    def delete_plugin(self, name):
        return self.request(
            "DELETE",
            self.plugin_path(name),
        )

    def upload_plugin(
        self,
        file,
        plugin_type,
        options=None,
        before=None,
        after=None,
    ):
        file_path = Path(file)

        if not file_path.is_file():
            raise ValueError(f"File does not exist: {file}")

        if plugin_type not in ("request_filter", "response_filter"):
            raise ValueError(f"Invalid plugin type: {plugin_type}")

        boundary = uuid.uuid4().hex

        with file_path.open("rb") as stream:
            file_content = stream.read()

        if len(file_content) > 1024 * 1024:
            raise ValueError("Plugin source exceeds the 1 MiB limit")

        filename = file_path.name.replace('"', '\\"')

        parts = [
            f"--{boundary}\r\n".encode(),
            (
                'Content-Disposition: form-data; '
                f'name="file"; filename="{filename}"\r\n'
            ).encode(),
            b"Content-Type: application/octet-stream\r\n\r\n",
            file_content,
            b"\r\n",
            f"--{boundary}--\r\n".encode(),
        ]

        body = b"".join(parts)

        params = {
            "type": plugin_type,
            "options": json.dumps(
                options if options is not None else {},
            ),
        }

        if before is not None:
            params["before"] = before

        if after is not None:
            params["after"] = after

        return self.request(
            "POST",
            "/admin/plugins",
            params=params,
            body=body,
            headers={
                "Content-Type": f"multipart/form-data; boundary={boundary}"
            },
        )


def parse_json(value):
    try:
        result = json.loads(value)

        if not isinstance(result, dict):
            raise ValueError("Expected a JSON object")

        return result

    except json.JSONDecodeError as error:
        raise argparse.ArgumentTypeError(
            f"Invalid JSON: {error}"
        )


def print_result(result):
    if result is None:
        print("OK")
        return

    print(json.dumps(
        result,
        indent=2,
        ensure_ascii=False,
    ))


def build_parser():
    parser = argparse.ArgumentParser(
        description="LLM Gateway CLI client"
    )

    parser.add_argument(
        "--host",
        default=os.getenv("LLM_GATEWAY_URL", DEFAULT_HOST),
        help="Gateway host (env: LLM_GATEWAY_URL)",
    )

    parser.add_argument(
        "--api-key",
        default=os.getenv("LLM_GATEWAY_API_KEY"),
        help="API key (env: LLM_GATEWAY_API_KEY)",
    )

    subparsers = parser.add_subparsers(
        dest="resource",
        required=True,
    )

    plugins = subparsers.add_parser("plugins")
    commands = plugins.add_subparsers(
        dest="command",
        required=True,
    )

    # plugins list
    commands.add_parser("list")

    # plugins get
    get = commands.add_parser("get")
    get.add_argument("name")

    # plugins upload
    upload = commands.add_parser("upload")
    upload.add_argument("file")

    upload.add_argument(
        "--type",
        required=True,
        choices=["request_filter", "response_filter"],
    )

    upload.add_argument("--options", type=parse_json)
    upload.add_argument("--before")
    upload.add_argument("--after")

    # plugins update
    update = commands.add_parser("update")
    update.add_argument("name")
    update.add_argument("--options", type=parse_json)
    update.add_argument("--before")
    update.add_argument("--after")

    # plugins patch
    patch = commands.add_parser("patch")
    patch.add_argument("name")

    patch.add_argument(
        "--enabled",
        choices=["true", "false"],
    )

    patch.add_argument("--before")
    patch.add_argument("--after")

    # plugins delete
    delete = commands.add_parser("delete")
    delete.add_argument("name")

    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()

    if not args.api_key:
        parser.error(
            "API key is required. Use --api-key or "
            "LLM_GATEWAY_API_KEY environment variable."
        )

    client = GatewayClient(
        host=args.host,
        api_key=args.api_key,
    )

    try:
        if args.command == "list":
            result = client.list_plugins()

        elif args.command == "get":
            result = client.get_plugin(args.name)

        elif args.command == "upload":
            result = client.upload_plugin(
                file=args.file,
                plugin_type=args.type,
                options=args.options,
                before=args.before,
                after=args.after,
            )

        elif args.command == "update":
            result = client.update_plugin(
                name=args.name,
                options=args.options,
                before=args.before,
                after=args.after,
            )

        elif args.command == "patch":
            enabled = (
                None if args.enabled is None
                else args.enabled == "true"
            )

            result = client.patch_plugin(
                name=args.name,
                enabled=enabled,
                before=args.before,
                after=args.after,
            )

        elif args.command == "delete":
            result = client.delete_plugin(args.name)

        else:
            parser.error("Unknown command")

        print_result(result)

    except ValueError as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
