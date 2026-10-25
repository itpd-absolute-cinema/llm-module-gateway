# Architecture

This architecture serves the scope defined in [Product Vision](../product-vision.md).

## Drivers

- **Threshold of Success:** [TOS-01 … TOS-nn](../threshold-of-success.md)
- **Constraints:** [## Constraints](../product-vision.md#constraints)
- **Functional requirements:** [Functional Requirements Index](../functional-requirements/README.md)
- **Quality requirements:** [Quality Requirements Index](../quality-requirements/README.md)

## Container View

The planned system runs as three deployable applications and one data store, and calls external LLM providers.
The current Python prototype instead runs as one `prototype gateway` process ([ADR-004](adr/ADR-004.md)); it does not implement the planned CLI or demonstrate the multi-service performance and isolation targets.

![Container diagram](container.svg)

The stack and the split are decided in [ADR-001](adr/ADR-001.md).

### Container Responsibilities

| Container | Technology | Responsibilities | Data / State Owned |
| --- | --- | --- | --- |
| CLI | Python | Provides a command-line interface for developers and DevOps engineers; sends requests to the Gateway over HTTP; displays responses and errors | None (stateless client) |
| Gateway | Java | Receives client requests via custom HTTP API; coordinates request processing through the Plugin Manager; routes requests to LLM providers; validates management changes | In-memory request context; persists desired plugin state in Plugin Store |
| Plugin Manager | Python | Discovers and executes plugins; inspects and processes requests and responses; returns results to the Gateway | Loaded plugin registry (in memory) |
| Plugin Store | File system (local directory) | Stores plugin files uploaded via the CLI; persists plugin source and configuration between restarts | Plugin files and desired configuration |

In the prototype, `prototype gateway` (Python / FastAPI / Uvicorn) owns the local TOML and plugin files, handles chat and admin requests, and loads the active chain at startup. It is a separate deployment scope, not an additional service in the planned system.

### External Systems

| System | Interaction |
| --- | --- |
| LLM Providers (OpenAI-compatible APIs) | Gateway routes requests and receives completions over HTTPS |
| CLI users (developers / DevOps) | Send commands and plugin files to the Gateway |

## Architectural Decisions

| ID | Title | Status | Addresses |
| --- | --- | --- | --- |
| [ADR-001](adr/ADR-001.md) | Split into CLI, Gateway, and Plugin Manager with a Java/Python stack and a shared plugin volume | Accepted | FR-01…FR-07, QR-01, QR-02 |
| [ADR-002](adr/ADR-002.md) | Apply plugin changes by restarting the Plugin Manager, reading files from the shared volume | Accepted | FR-01, FR-02, FR-03, FR-04, FR-05, FR-06, FR-009, QR-02, QR-03, QR-05 |
| [ADR-003](adr/ADR-003.md) | Expose plugin management as a Gateway HTTP API consumed by a thin CLI | Accepted | FR-01…FR-05, FR-009, QR-01, QR-03, QR-05 |
| [ADR-004](adr/ADR-004.md) | Keep prototype administration in one Python process | Accepted | [FR-01](../functional-requirements/fr/FR-01.md), [QR-03](../quality-requirements/qr/QR-03.md), [QR-05](../quality-requirements/qr/QR-05.md) |
| [ADR-005](adr/ADR-005.md) | Authorize administration independently of plugins | Accepted | [FR-008](../functional-requirements/fr/FR-008.md), [QR-04](../quality-requirements/qr/QR-04.md) |
| [ADR-006](adr/ADR-006.md) | Retain disabled settings and order in TOML | Accepted | [FR-01](../functional-requirements/fr/FR-01.md), [FR-02](../functional-requirements/fr/FR-02.md), [FR-03](../functional-requirements/fr/FR-03.md), [FR-04](../functional-requirements/fr/FR-04.md), [FR-009](../functional-requirements/fr/FR-009.md), [QR-05](../quality-requirements/qr/QR-05.md) |

[ADR template](adr/TEMPLATE.md).

## Diagrams And Legends

Existing assets are retained without regeneration. Despite their filenames, they depict system context; a verified container-level image remains a separate documentation task.

**Legend:**

- Rectangle = application container
- Cylinder = data store
- Dashed rectangle = external system
- Solid arrow = synchronous HTTP call; label states what crosses it

**Tool:** PlantUML (C4-PlantUML)

**Sources:**

- Diagram image: `docs/architecture/container.svg`
- Diagram source: `docs/architecture/container.puml`
