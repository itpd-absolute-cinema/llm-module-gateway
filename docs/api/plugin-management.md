# Plugin Management

How DevOps manages plugins in the LLM Gateway. This is the control plane: it changes which plugins run, not how LLM requests are processed.

## Components

- **CLI** (Python) — the DevOps entry point.
- **Gateway** (Java) — exposes the plugin management HTTP API and writes uploaded files to the shared plugin volume.
- **Plugin Manager** (Python) — scans the shared volume on startup, registers plugins, and owns the in-memory registry of enabled/disabled plugins.

Plugin files live on a shared volume mounted into the Gateway and the Plugin Manager. Applying an uploaded plugin requires restarting the Plugin Manager, not the Gateway (see ADR-002).

## Operations

The Gateway API exposes upload, list, enable, disable, and delete. The CLI is a thin client over this API. Enable and disable take effect in memory without a restart; upload and delete require a Plugin Manager restart to scan the volume.

## Rules

- Every operation goes through the Gateway API; the CLI never touches the shared volume directly (ADR-003).
- A plugin is identified by its name; duplicate names are rejected.
- Unknown names return an error naming the missing plugin.
- The enabled set the Gateway applies is exactly the set DevOps configured (QR-01).
