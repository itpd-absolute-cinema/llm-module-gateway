## Architectural Decisions

| ID | Title | Status | Addresses |
|---|---|---|---|
| [ADR-001](adr/ADR-001.md) | Split into CLI, Gateway, and Plugin Manager with a Java/Python stack and a shared plugin volume | Accepted | FR-01…FR-07, QR-01, QR-02 |
| [ADR-002](adr/ADR-002.md) | Apply plugin changes by restarting the Plugin Manager, reading files from the shared volume | Accepted | FR-01, FR-02, FR-06, QR-02, QR-03 |
| [ADR-003](adr/ADR-003.md) | Expose plugin management as a Gateway HTTP API consumed by a thin CLI | Accepted | FR-01…FR-05, QR-01, QR-03 |