# LLM Gateway CLI

A lightweight command-line client for the **LLM Gateway API**.

The CLI provides convenient access to plugin administration, including uploading, configuring, enabling, disabling, inspecting, and deleting plugins.

Built with Python's standard library. No third-party dependencies required.

## Features

- List installed plugins.
- Get plugin information.
- Upload new plugins.
- Update plugin settings.
- Enable or disable plugins.
- Delete plugins.
- Configure API host and authentication token.
- Support for JSON configuration options.
- Environment variable configuration.
- No external Python dependencies.

## Requirements

- Python 3.9+
- Access to an LLM Gateway API instance.
- A valid API key.

## Installation

Make the script executable (Linux/macOS):

```bash
chmod +x llm_gateway_cli.py
```

## Configuration

The CLI supports configuration through command-line arguments and environment variables.

### Environment variables

| Variable | Description | Default |
| --- | --- | --- |
| `LLM_GATEWAY_URL` | Gateway API base URL | `http://localhost:8080` |
| `LLM_GATEWAY_API_KEY` | API authentication key | - |

Example (Linux/macOS):

```bash
export LLM_GATEWAY_URL="http://localhost:8080"
export LLM_GATEWAY_API_KEY="secret"
```

Example (Windows PowerShell):

```powershell
$env:LLM_GATEWAY_URL = "http://localhost:8080"
$env:LLM_GATEWAY_API_KEY = "secret"
```

### Command-line arguments

Configuration can also be supplied directly:

```bash
python llm_gateway_cli.py \
    --host https://gateway.example.com \
    --api-key your-api-key \
    plugins list
```

Command-line arguments take precedence over environment variables.

## Usage

General syntax:

```bash
python llm_gateway_cli.py [GLOBAL OPTIONS] plugins COMMAND [OPTIONS]
```

Available global options:

| Option | Description |
| --- | --- |
| `--host` | Gateway API base URL |
| `--api-key` | API authentication key |
| `--help` | Display help information |

## Plugin Management

### List plugins

Retrieve all registered plugins:

```bash
python llm_gateway_cli.py plugins list
```

Example response:

```json
[
  {
    "name": "example-plugin",
    "enabled": true,
    "options": {
      "temperature": 0.7
    }
  }
]
```

### Get plugin information

Retrieve information about a specific plugin:

```bash
python llm_gateway_cli.py plugins get example-plugin
```

### Upload a plugin

Upload a plugin archive or binary:

```bash
python llm_gateway_cli.py plugins upload ./example-plugin.py
```

Additional options can be supplied during upload:

```bash
python llm_gateway_cli.py plugins upload ./example-plugin.py \
    --options '{"phone_mask": "[MASKED]", "phone_pattern": "..."}' \
    --before "another-plugin"
```

| Option | Description |
| --- | --- |
| `file` | Path to the plugin file (required) |
| `--options` | Plugin configuration as a JSON object |
| `--before` | Plugin ordering reference |
| `--after` | Plugin ordering reference |

### Update plugin settings

Update the configuration of an existing plugin:

```bash
python llm_gateway_cli.py plugins update example-plugin \
    --options '{"temperature": 0.5}'
```

You can also specify execution ordering:

```bash
python llm_gateway_cli.py plugins update example-plugin \
    --options '{"temperature": 0.5}' \
    --before "another-plugin"
```

Supported options:

| Option | Description |
| --- | --- |
| `name` | Plugin name (required) |
| `--options` | Plugin configuration as a JSON object |
| `--before` | Plugin ordering reference |
| `--after` | Plugin ordering reference |

### Enable or disable a plugin

Enable:

```bash
python llm_gateway_cli.py plugins patch example-plugin --enabled true
```

Disable:

```bash
python llm_gateway_cli.py plugins patch example-plugin --enabled false
```

You can also update ordering:

```bash
python llm_gateway_cli.py plugins patch example-plugin \
    --enabled true \
    --before "another-plugin"
```

### Delete a plugin

Delete a plugin from the Gateway:

```bash
python llm_gateway_cli.py plugins delete example-plugin
```

The API returns `204 No Content` on successful deletion.

## API Reference

The CLI currently supports the following endpoints:

| Method | Endpoint | CLI command |
| --- | --- | --- |
| GET | `/admin/plugins` | `plugins list` |
| GET | `/admin/plugins/{name}` | `plugins get` |
| POST | `/admin/plugins` | `plugins upload` |
| PUT | `/admin/plugins/{name}` | `plugins update` |
| PATCH | `/admin/plugins/{name}` | `plugins patch` |
| DELETE | `/admin/plugins/{name}` | `plugins delete` |

### Authentication

All endpoints use HTTP Bearer authentication.

The CLI automatically adds the following HTTP header:

```http
Authorization: Bearer <API_KEY>
```

### Request formats

- `GET`: No request body.
- `POST`: Multipart form data containing the plugin file. Additional settings are passed as query parameters.
- `PUT`: JSON request body containing plugin settings.
- `PATCH`: JSON request body containing plugin status and ordering.
- `DELETE`: No request body.

## Error Handling

The CLI reports API errors and connection failures to standard error.

Examples:

```text
HTTP 401: Unauthorized
```

```text
Connection error: [Errno 111] Connection refused
```

For HTTP errors, the response body is also printed when available.

A non-zero exit code is returned when an operation fails.

## Project Structure

```text
cli/
├── llm_gateway_cli.py
└── README.md
```
