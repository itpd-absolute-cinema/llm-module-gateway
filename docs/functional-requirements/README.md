# Functional Requirements

Index of functional requirements in `FR-nnn` order. FR-01…FR-05 cover the plugin management API (control plane); FR-06…FR-07 cover LLM request processing (data plane).

`Active` means in scope, not implemented. The prototype provides a single-process administration API;
the CLI and separate Gateway/Plugin Manager are planned.

The Gateway owns saved writes to Plugin Store. GET reports running status and options with a
per-plugin `restart_required` flag; write responses describe saved state. Runtime restart means
restarting the prototype process today or the Plugin Manager in the planned deployment.

API checks do not establish CLI completion, multi-provider routing, or the planned plugin-failure policy.

| ID | Title | Status | Traces to | Containers |
| --- | --- | --- | --- | --- |
| [FR-01](fr/FR-01.md) | DevOps uploads a new plugin through the CLI | Active | TOS-02; modular plugin system | CLI, Gateway, Plugin Manager, Plugin Store |
| [FR-02](fr/FR-02.md) | DevOps lists all plugins and their statuses through the CLI | Active | plugin state observability | CLI, Gateway, Plugin Manager |
| [FR-03](fr/FR-03.md) | DevOps enables a plugin by name through the CLI | Active | filter/validate; plugin activation | CLI, Gateway, Plugin Manager |
| [FR-04](fr/FR-04.md) | DevOps disables a plugin by name through the CLI | Active | configurable policies; operational control | CLI, Gateway, Plugin Manager |
| [FR-05](fr/FR-05.md) | DevOps deletes a plugin by name through the CLI | Active | modular plugin system; lifecycle | CLI, Gateway, Plugin Manager, Plugin Store |
| [FR-06](fr/FR-06.md) | LLM requests pass through all enabled plugins before reaching the provider | Active | TOS-01; filter/validate; plugin system | Gateway, Plugin Manager |
| [FR-07](fr/FR-07.md) | Gateway routes the allowed request to the configured LLM provider | Active | route to appropriate providers | Gateway |
| [FR-008](fr/FR-008.md) | Separate Admin Authorization | Active | DEC-007; CON-04; ADR-005 | prototype gateway |
| [FR-009](fr/FR-009.md) | Replace Plugin Options | Active | CON-01; DEC-001; ADR-002; ADR-003; ADR-006 | prototype gateway |
