# LLM Gateway CLI

Command-line client for administering plugins of the LLM Gateway. Python standard library only, no dependencies.

**Requires:** Python 3.9+, a running gateway, a Keycloak account with the `gateway-admin` role.

## Quick start

```bash
alias gw='python3 llm_gateway_cli.py'     # the examples below use `gw`
export LLM_GATEWAY_URL=http://localhost:8080

gw login                                   # sign in through the browser
gw plugins upload mask_phones.py --type request_filter
gw plugins patch mask_phones --enabled true
gw plugins list
```

## Commands

| Command | What it does |
| --- | --- |
| `gw login` | Sign in through the browser (device flow) |
| `gw logout` | End the session and delete the saved token |
| `gw whoami` | Show the user, roles and token expiry |
| `gw plugins list` | List all plugins in chain order |
| `gw plugins get NAME` | Show one plugin |
| `gw plugins upload FILE --type TYPE` | Add a plugin (created disabled) |
| `gw plugins update NAME` | Replace a plugin's settings and/or move it |
| `gw plugins patch NAME` | Enable or disable a plugin and/or move it |
| `gw plugins delete NAME` | Delete a disabled plugin and its file |

### login / logout / whoami

```bash
gw login                # opens the browser; prints the URL and code if it cannot
gw login --no-browser   # only print the URL and code
gw whoami               # user, roles, expiry
gw logout
```

### plugins list / get

```bash
gw plugins list
gw plugins get mask_phones
```

### plugins upload

The plugin name is the file name without `.py`. New plugins are disabled and go to the end of the chain unless `--before` or `--after` is given.

```bash
gw plugins upload mask_phones.py --type request_filter \
    --options '{"phone_mask": "[MASKED]"}' --before validate_input
```

| Option | Description |
| --- | --- |
| `FILE` | Python file, up to 1 MiB (required) |
| `--type` | `request_filter` or `response_filter` (required, cannot be changed later) |
| `--options` | Plugin settings, a JSON object |
| `--before` / `--after` | Insert next to an existing plugin (not both) |

### plugins update

Replaces the settings completely (not a merge) and can move the plugin.

```bash
gw plugins update mask_phones --options '{"phone_mask": "***"}'
gw plugins update mask_phones --options '{}' --after validate_input
```

| Option | Description |
| --- | --- |
| `--options` | New settings, a JSON object |
| `--before` / `--after` | Move next to another plugin (not both) |

### plugins patch

Changes the state or the position; the settings stay as they are.

```bash
gw plugins patch mask_phones --enabled true
gw plugins patch mask_phones --enabled false
gw plugins patch mask_phones --before validate_input
```

| Option | Description |
| --- | --- |
| `--enabled` | `true` or `false` |
| `--before` / `--after` | Move next to another plugin (not both) |

### plugins delete

```bash
gw plugins delete mask_phones     # the plugin must be disabled first
```

## Authentication

| Mode | When | How |
| --- | --- | --- |
| **Browser login** | A person at a terminal | `gw login`; the token is saved and refreshed automatically |
| **Service account** | Scripts and CI | Set `LLM_GATEWAY_CLIENT_ID` and `LLM_GATEWAY_CLIENT_SECRET`; a token is requested on every run, nothing is stored |
| **Ready token** | Debugging | `--token` or `LLM_GATEWAY_TOKEN` |

If several are set, the order is: ready token, then service account, then the saved login.

The saved session lives in `~/.config/llm-gateway/token.json` (mode `600`) and holds a refresh token, so treat it like a password. After about 30 minutes without use, run `gw login` again.

## Configuration

| Option | Environment variable | Default |
| --- | --- | --- |
| `--host` | `LLM_GATEWAY_URL` | `http://localhost:8080` |
| `--issuer` | `LLM_GATEWAY_ISSUER` | `http://localhost:8091/realms/gateway` |
| `--client-id` | `LLM_GATEWAY_CLIENT_ID` | `gateway-cli` |
| `--token` | `LLM_GATEWAY_TOKEN` | none |
| none (environment only) | `LLM_GATEWAY_CLIENT_SECRET` | none |

Command-line options override environment variables. The client secret has no option on purpose: arguments are visible to other users.

## Errors and exit codes

Every failure prints a message to stderr and exits with code `1`; success exits with `0`. For HTTP errors the response body is printed too.

| Message | Likely cause |
| --- | --- |
| `Not logged in, or the session has expired` | Run `gw login` |
| `HTTP 401` | The token expired, or its issuer does not match the gateway's |
| `HTTP 403` | The token is valid but the user lacks the `gateway-admin` role |
| `unauthorized_client` at login | Enable *OAuth 2.0 Device Authorization Grant* for the client in Keycloak |
| `Cannot reach the identity provider` | Wrong `--issuer`, or Keycloak is not running |
| `Connection error` | Wrong `--host`, or the gateway is not running |

## Command to API map

| Command | Request |
| --- | --- |
| `plugins list` | `GET /admin/plugins` |
| `plugins get` | `GET /admin/plugins/{name}` |
| `plugins upload` | `POST /admin/plugins` (multipart file; type, options, ordering as query parameters) |
| `plugins update` | `PUT /admin/plugins/{name}` |
| `plugins patch` | `PATCH /admin/plugins/{name}` |
| `plugins delete` | `DELETE /admin/plugins/{name}` |

All requests carry `Authorization: Bearer <JWT>`.