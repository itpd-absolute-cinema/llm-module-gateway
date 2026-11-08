#!/usr/bin/env python3

import argparse
import base64
import json
import os
import sys
import time
import uuid
import urllib.error
import urllib.parse
import urllib.request
import webbrowser

from datetime import datetime, timezone
from pathlib import Path


DEFAULT_HOST = "http://localhost:8080"
DEFAULT_ISSUER = "http://localhost:8091/realms/gateway"
DEFAULT_CLIENT_ID = "gateway-cli"

# a token that expires within this many seconds is treated as already expired
EXPIRY_LEEWAY = 30


class AuthError(Exception):
    pass


def default_token_file():
    base = os.getenv("XDG_CONFIG_HOME") or str(Path.home() / ".config")
    return Path(base) / "llm-gateway" / "token.json"


def decode_claims(token):
    """Reads the claims of a JWT without verifying it (for display only: the gateway verifies tokens)."""
    try:
        payload = token.split(".")[1]
        payload += "=" * (-len(payload) % 4)
        return json.loads(base64.urlsafe_b64decode(payload))
    except (IndexError, ValueError):
        raise AuthError("The token is not a valid JWT")


class KeycloakAuth:
    def __init__(self, issuer, client_id, client_secret=None, token_file=None):
        self.issuer = issuer.rstrip("/")
        self.client_id = client_id
        self.client_secret = client_secret
        self.token_file = token_file or default_token_file()

    @property
    def token_url(self):
        return f"{self.issuer}/protocol/openid-connect/token"

    @property
    def device_url(self):
        return f"{self.issuer}/protocol/openid-connect/auth/device"

    @property
    def logout_url(self):
        return f"{self.issuer}/protocol/openid-connect/logout"

    # ---------- token resolution ----------

    def access_token(self):
        """Returns a valid access token: from client credentials, from the stored session, or after a refresh."""
        if self.client_secret:
            return self.client_credentials_token()["access_token"]

        stored = self._load()
        now = time.time()

        if stored and stored["expires_at"] - EXPIRY_LEEWAY > now:
            return stored["access_token"]

        if (
            stored
            and stored.get("refresh_token")
            and stored.get("refresh_expires_at", 0) - EXPIRY_LEEWAY > now
        ):
            refreshed = self._refresh(stored)

            if refreshed:
                return refreshed["access_token"]

        raise AuthError(
            "Not logged in, or the session has expired. "
            "Run the 'login' command."
        )

    # ---------- flows ----------

    def login_device(self, open_browser=True):
        """OAuth2 device authorization grant: the user confirms the login in a browser."""
        status, device = self._post_form(
            self.device_url,
            {"client_id": self.client_id},
        )

        if status != 200:
            raise AuthError(self._describe(device, "Device login is not available"))

        url = device.get("verification_uri_complete") or device["verification_uri"]

        print("Open this page in a browser and sign in:")
        print(f"  {url}")
        print(f"Confirmation code: {device['user_code']}")

        if open_browser:
            webbrowser.open(url)

        interval = int(device.get("interval", 5))
        deadline = time.time() + int(device.get("expires_in", 600))

        print("Waiting for confirmation...", file=sys.stderr)

        while time.time() < deadline:
            time.sleep(interval)

            status, payload = self._post_form(
                self.token_url,
                {
                    "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id": self.client_id,
                    "device_code": device["device_code"],
                },
            )

            if status == 200:
                return self._save(payload)

            error = payload.get("error")

            if error == "authorization_pending":
                continue

            if error == "slow_down":
                interval += 5
                continue

            raise AuthError(self._describe(payload, "Login failed"))

        raise AuthError("Login timed out. Run the 'login' command again.")

    def client_credentials_token(self):
        """For scripts and CI: a service account signs in with a secret. Nothing is stored."""
        status, payload = self._post_form(
            self.token_url,
            {
                "grant_type": "client_credentials",
                "client_id": self.client_id,
                "client_secret": self.client_secret,
            },
        )

        if status != 200:
            raise AuthError(self._describe(payload, "Client credentials were rejected"))

        return payload

    def logout(self):
        stored = self._load()

        if stored and stored.get("refresh_token"):
            try:
                # best effort: end the session on the server as well
                self._post_form(
                    self.logout_url,
                    {
                        "client_id": self.client_id,
                        "refresh_token": stored["refresh_token"],
                    },
                )
            except AuthError:
                pass

        try:
            self.token_file.unlink()
        except FileNotFoundError:
            pass

    # ---------- internals ----------

    def _refresh(self, stored):
        status, payload = self._post_form(
            self.token_url,
            {
                "grant_type": "refresh_token",
                "client_id": self.client_id,
                "refresh_token": stored["refresh_token"],
            },
        )

        if status != 200:
            return None

        return self._save(payload)

    def _load(self):
        try:
            stored = json.loads(self.token_file.read_text())
        except (OSError, ValueError):
            return None

        if (
            stored.get("issuer") != self.issuer
            or stored.get("client_id") != self.client_id
        ):
            return None

        return stored

    def _save(self, payload):
        now = time.time()
        refresh_token = payload.get("refresh_token")

        stored = {
            "issuer": self.issuer,
            "client_id": self.client_id,
            "access_token": payload["access_token"],
            "expires_at": now + payload.get("expires_in", 0),
            "refresh_token": refresh_token,
            "refresh_expires_at": (
                now + payload.get("refresh_expires_in", 0)
                if refresh_token else 0
            ),
        }

        self.token_file.parent.mkdir(parents=True, exist_ok=True, mode=0o700)

        # the file holds credentials: only the owner may read it
        fd = os.open(
            self.token_file,
            os.O_WRONLY | os.O_CREAT | os.O_TRUNC,
            0o600,
        )

        with os.fdopen(fd, "w") as stream:
            json.dump(stored, stream)

        os.chmod(self.token_file, 0o600)

        return stored

    def _post_form(self, url, data):
        request = urllib.request.Request(
            url,
            data=urllib.parse.urlencode(data).encode("utf-8"),
            headers={
                "Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json",
            },
            method="POST",
        )

        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.status, self._json(response.read())

        except urllib.error.HTTPError as error:
            # Keycloak reports expected states (for example "authorization_pending") as 400 with a JSON body
            payload = self._json(error.read())
            payload.setdefault("error", error.reason)
            return error.code, payload

        except urllib.error.URLError as error:
            raise AuthError(
                f"Cannot reach the identity provider at {self.issuer}: {error.reason}"
            )

        except TimeoutError:
            raise AuthError("The identity provider did not answer in time")

    @staticmethod
    def _json(raw):
        try:
            result = json.loads(raw) if raw else {}
        except ValueError:
            return {}

        return result if isinstance(result, dict) else {}

    @staticmethod
    def _describe(payload, default):
        return payload.get("error_description") or payload.get("error") or default


class GatewayClient:
    def __init__(self, host: str, token: str):
        self.host = host.rstrip("/")
        self.token = token

    def request(self, method, path, *, params=None, body=None, headers=None):
        url = self.host + path

        if params:
            query = urllib.parse.urlencode(params)
            url += "?" + query

        request_headers = {
            "Authorization": f"Bearer {self.token}",
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

            if error.code == 401:
                print(
                    "Hint: the token was not accepted. "
                    "Run the 'login' command again.",
                    file=sys.stderr,
                )
            elif error.code == 403:
                print(
                    "Hint: the token is valid but lacks the 'gateway-admin' role.",
                    file=sys.stderr,
                )

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


def describe_token(token):
    claims = decode_claims(token)
    exp = claims.get("exp")

    return {
        "user": claims.get("preferred_username", claims.get("sub")),
        "roles": claims.get("realm_access", {}).get("roles", []),
        "issuer": claims.get("iss"),
        "expires_at": (
            datetime.fromtimestamp(exp, tz=timezone.utc).isoformat()
            if exp else None
        ),
    }


def login(auth, args):
    if auth.client_secret:
        auth.client_credentials_token()
        print(
            f"Client credentials of '{auth.client_id}' are valid. "
            "No login is needed: a token is requested automatically on every run."
        )
        return

    stored = auth.login_device(open_browser=not args.no_browser)
    claims = decode_claims(stored["access_token"])
    print(f"Logged in as {claims.get('preferred_username', claims.get('sub'))}.")


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
        "--issuer",
        default=os.getenv("LLM_GATEWAY_ISSUER", DEFAULT_ISSUER),
        help="Keycloak realm URL (env: LLM_GATEWAY_ISSUER)",
    )

    parser.add_argument(
        "--client-id",
        default=os.getenv("LLM_GATEWAY_CLIENT_ID", DEFAULT_CLIENT_ID),
        help="Keycloak client (env: LLM_GATEWAY_CLIENT_ID)",
    )

    parser.add_argument(
        "--token",
        default=os.getenv("LLM_GATEWAY_TOKEN"),
        help="Use this access token instead of logging in (env: LLM_GATEWAY_TOKEN)",
    )

    subparsers = parser.add_subparsers(
        dest="resource",
        required=True,
    )

    login_parser = subparsers.add_parser(
        "login",
        help="Sign in through the browser",
    )

    login_parser.add_argument(
        "--no-browser",
        action="store_true",
        help="Only print the sign-in URL, do not open a browser",
    )

    subparsers.add_parser("logout", help="Forget the saved session")
    subparsers.add_parser("whoami", help="Show who the current token belongs to")

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

    # the secret is read only from the environment: a command-line flag would be visible to other users
    auth = KeycloakAuth(
        issuer=args.issuer,
        client_id=args.client_id,
        client_secret=os.getenv("LLM_GATEWAY_CLIENT_SECRET"),
    )

    try:
        if args.resource == "login":
            login(auth, args)
            return

        if args.resource == "logout":
            auth.logout()
            print("Logged out.")
            return

        token = args.token or auth.access_token()

        if args.resource == "whoami":
            print_result(describe_token(token))
            return

        client = GatewayClient(
            host=args.host,
            token=token,
        )

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

    except AuthError as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(1)

    except ValueError as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()