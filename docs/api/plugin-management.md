# Plugin management API

The Python prototype exposes `/admin/plugins` through a separate FastAPI router.
The Gateway owns writes to saved configuration and plugin files; it does not reload the running pipeline.
The prototype runs administration and plugin execution in the same process.
See the [architecture and ADRs](../architecture/README.md) for the decisions and the [prototype README](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/prototype/README.md) for startup commands and plugin examples.

## Authorization and scope

Set `GATEWAY_ADMIN_API_KEY` and send `Authorization: Bearer <admin-key>` on every admin request.
Choose a different value from `GATEWAY_API_KEY`; equality is not prevented by the server.
For valid-shaped requests, missing or wrong credentials return 401; an unset admin key returns 503.
Malformed bodies can fail validation with 400 before authorization runs.
Disabling chat authentication does not disable admin authorization.

Use one worker and trusted plugins only: uploading Python grants full process access.
The API manages single `.py` modules in `prototype/gateway/plugins/`, including bundled plugins.
Names omit `.py`; path traversal, symlinks, and package initializers are rejected.
Existing source files cannot be overwritten. Uploads have a 1 MiB source limit after multipart parsing.
Protect exposed deployments with TLS, network restrictions, and a request-body limit.

## Operations

| Method | Path | Input and result |
| --- | --- | --- |
| POST | `/admin/plugins` | Multipart `file`, optional `options` JSON-object string, and optional `before` or `after` name; creates a disabled plugin, 201 |
| GET | `/admin/plugins` | Alphabetical list of installed, configured, or active local plugins with running state, 200 |
| GET | `/admin/plugins/{name}` | One running-state object: `{name, enabled, options, restart_required}`, 200 |
| PUT | `/admin/plugins/{name}` | `{"options": {...}}` with optional `before` or `after`; replaces all options, preserves enabled state, 200 |
| PATCH | `/admin/plugins/{name}` | `enabled` and/or one of `before`, `after`; preserves options, 200 |
| DELETE | `/admin/plugins/{name}` | Removes inactive source and configuration, 204; disable and restart first if running |

For an existing plugin named `prefix`, this changes only its saved enabled flag:

```sh
curl -i -X PATCH http://127.0.0.1:8000/admin/plugins/prefix \
  -H 'Authorization: Bearer local-demo-admin-key' \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true}'
```

Use localhost-only sample credentials; restart the gateway to apply this change.
There is no whole-config GET/POST. Edit upstream settings in TOML;
[ADR-003](../architecture/adr/ADR-003.md) explains why clients do not replace the entire document.

## Relative ordering

POST, PUT, and PATCH accept either `before` or `after`, naming a configured local plugin
without `.py`. The operation places the entry immediately next to that target in the
TOML list, including disabled entries. For example, an order-only PATCH body is:

```json
{"before": "literal_redaction"}
```

To enable and move a plugin together, use `{"enabled": true, "after": "authentication"}`.
PUT still requires `options`; POST takes ordering hints as multipart form fields.
Without a hint, new entries append and updates preserve their position. JSON `null`
hints count as omitted; an empty PATCH or `enabled: null` is invalid.

Both hints together and self-references return 400. A target must have a configuration
entry, even if its source file exists; otherwise the request returns 404. Ambiguous
targets with multiple entries return 409. The API saves placement and options/status
together, so a rejected move does not partially update the plugin.

These are one-time moves, not persistent dependencies. Repeating a move without
intervening changes leaves the configuration unchanged; another administrator can
still change the order between requests. Requests enter in TOML order and responses
return in reverse. GET listings remain alphabetical, not execution order.
Ordering changes affect the running chain only after restart.

## Saved state and restart

Both GET routes return `{name, enabled, options, restart_required}` for each plugin.
`enabled` and `options` come from the enabled startup snapshot, not saved configuration.
Inactive plugins, including newly uploaded ones, return `enabled: false` and `options: {}`.
GET does not expose their saved options.

The per-plugin `restart_required` compares the saved enabled chain with the startup chain:

- Whether the plugin is present in each chain.
- Its options, including value types (`1`, `1.0`, and `true` are distinct).
- Its numeric position among enabled plugins, not its position among all TOML entries.

A position shift can flag a plugin even if another plugin caused it.
Uploading a disabled plugin or changing its settings alone does not require restart.
A plugin disabled in saved configuration but still running remains flagged until restart.

For example, after saving a disable for a running `prefix` plugin, GET still returns:

```json
{
  "name": "prefix",
  "enabled": true,
  "options": {"prefix": "Old prefix: "},
  "restart_required": true
}
```

It keeps using these options until restart. After restart, GET returns `enabled: false`,
`options: {}`, and `restart_required: false`. To delete it, disable, restart, then delete.

POST, PUT, and PATCH return the saved `{name, enabled, options}` object without a
`restart_required` body field. Use those responses or TOML to inspect saved changes;
there is no separate desired-state GET endpoint.

In saved configuration, a file without a config entry is disabled with empty options.
Configured entries default to enabled for compatibility; `enabled = false` retains options and position.
PUT replaces rather than merges options. New entries append unless an ordering hint is supplied.
Options must fit TOML, so JSON `null` is rejected. Use environment-variable references for secrets.

Successful admin responses include `X-Restart-Required: true` when saved enabled entries differ from the startup snapshot, otherwise `false`.
This flag is process-wide, including on list responses; it does not mean that a change has been activated.
Reverting all pending changes clears the header flag.
Uploads check Python syntax without executing code; factories, dependencies, and plugin-specific options are checked only at startup.
Invalid saved settings can prevent the next startup. Correct them through the API before restarting, or edit TOML afterward.

The server preserves unrelated TOML settings and detects external file edits before saving, returning 409 until restart.
API mutations are serialized within one process; concurrent external editors and multiple workers are unsupported.
Two updates to the same plugin's options remain last-write-wins, not version-checked.
Recoverable write failures leave the previous saved state intact; this is not a multi-file crash-recovery guarantee.

## Errors and schema

The route declarations document these admin errors where applicable:

| Status | Meaning |
| --- | --- |
| 400 | Malformed request, invalid plugin name or Python source, symlink plugin, invalid ordering hint, or upload options that are not a JSON object |
| 401 | Missing or invalid admin bearer credentials |
| 404 | Plugin not found, or ordering target has no configuration entry |
| 409 | Duplicate plugin, multiple configuration entries, external config edit, or deletion while enabled/running |
| 413 | Uploaded plugin source exceeds 1 MiB |
| 422 | Upload or PUT options cannot be represented in TOML, such as JSON `null` |
| 500 | Plugin storage operation failed |
| 503 | `GATEWAY_ADMIN_API_KEY` is unset |

Request-validation failures return 400, not FastAPI's default 422.
Errors use the gateway's `error` envelope with `message`, `type`, `param`, and `code`, rather than echoing submitted values.
All handled errors include `X-Request-ID`; 401 responses also include `WWW-Authenticate: Bearer`.

Download the [OpenAPI YAML](openapi.yaml), regenerated on 2026-10-25 through the application's `/openapi.json` endpoint using an in-process test client.
It contains the full served schema, including chat and health routes, without manual edits.
The schema documents per-operation errors, their shared body, and the `X-Restart-Required` success header.
Chat errors include 400 for invalid requests, 401 for authentication, 403 for a blocked model, 429 for upstream rate limiting, 500 for gateway/plugin failure, 502 for upstream connection or non-429 error responses, and 504 for upstream timeouts.

To refresh the snapshot from a running gateway, use the documentation environment from the repository root:

```sh
.venv-docs/bin/python -c 'import json, pathlib, urllib.request, yaml; response = urllib.request.urlopen("http://127.0.0.1:8000/openapi.json", timeout=10); schema = json.load(response); pathlib.Path("docs/api/openapi.yaml").write_text(yaml.safe_dump(schema, sort_keys=False, allow_unicode=True), encoding="utf-8")'
```

Run the behavior checks from `prototype/` with `uv run --locked python -m unittest discover -s tests -v`.
