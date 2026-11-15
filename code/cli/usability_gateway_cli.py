#!/usr/bin/env python3
"""Run the Gateway CLI with a local event log for UT-01 on staging."""

import json
import os
import sys
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

from llm_gateway_cli import GatewayClient, main, print_result


SYNTHETIC_INPUT = "Contact: +1 202-555-0100"


class StudyClient(GatewayClient):
    def __init__(self, host, token, staging_url, session_id, event_log):
        super().__init__(host, token)
        if self.host != staging_url.rstrip("/"):
            raise ValueError("Use the configured UT-01 staging Gateway")
        self.session_id = session_id
        self.event_log = event_log

    def request(self, method, path, *, params=None, body=None, headers=None):
        if path == "/chat/completions":
            action = "probe"
        elif method == "PATCH" and isinstance(body, dict) and "enabled" in body:
            action = "enable" if body["enabled"] else "disable"
        else:
            action = {
                "GET": "inspect",
                "POST": "upload",
                "PUT": "update",
                "PATCH": "patch",
                "DELETE": "delete",
            }[method]

        started = time.monotonic()
        event = {"outcome": "error"}
        try:
            response = super().request(method, path, params=params, body=body, headers=headers)
            if action == "probe":
                content = response["choices"][0]["message"]["content"]
                event["probe_effect"] = {
                    "[stub] Contact: [PHONE]": "masked",
                    f"[stub] {SYNTHETIC_INPUT}": "unmasked",
                }.get(content, "unexpected")
            event["outcome"] = "ok"
            return response
        finally:
            event.update(
                source="usability-test",
                session_id=self.session_id,
                action=action,
                timestamp=datetime.now(timezone.utc).isoformat(),
                duration_ms=round((time.monotonic() - started) * 1000),
            )
            self.event_log.write(json.dumps(event) + "\n")
            self.event_log.flush()


def run():
    try:
        staging_url = os.environ["UT01_STAGING_URL"]
        if not staging_url.startswith(("https://", "http://")):
            raise ValueError("UT01_STAGING_URL must be an HTTP(S) staging URL")
        if os.environ.get("LLM_GATEWAY_URL", "").rstrip("/") != staging_url.rstrip("/"):
            raise ValueError("Set LLM_GATEWAY_URL to UT01_STAGING_URL")
        session_id = str(uuid.UUID(os.environ["UT01_SESSION_ID"]))
        log_path = Path(os.environ["UT01_EVENT_LOG"]).expanduser()
        with log_path.open("a", encoding="utf-8") as event_log:
            def client_factory(host, token):
                return StudyClient(host, token, staging_url, session_id, event_log)

            if sys.argv[1:] == ["probe"]:
                client = client_factory(staging_url, os.getenv("LLM_GATEWAY_TOKEN", ""))
                result = client.request("POST", "/chat/completions", body={
                    "model": "ut01-stub",
                    "messages": [{"role": "user", "content": SYNTHETIC_INPUT}],
                })
                print_result(result)
            else:
                main(client_factory)
    except (KeyError, ValueError, OSError, TypeError, IndexError) as error:
        sys.exit(f"UT-01: {error}")


if __name__ == "__main__":
    run()
