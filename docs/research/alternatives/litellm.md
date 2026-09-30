## ALT-01: LiteLLM

**Kind:** Open-source LLM gateway

**Link:** [https://github.com/BerriAI/litellm](https://github.com/BerriAI/litellm)

**Depth of evaluation:** Documentation, repository, architecture, configuration and usage examples.

**Problem it solves:**
LiteLLM provides a single OpenAI-compatible interface to 100+ LLM providers, available both as a Python SDK and as a self-hosted proxy server. The proxy adds a central control layer on top: virtual API keys, budgets, rate limits, routing with fallbacks and load balancing, and spend tracking. It is designed so that applications talk to one endpoint while the platform team controls which models are used, by whom, and at what cost.

### Observations by property

| Property | Observation |
|---|---|
| Data control | Self-hosted: traffic goes from the application through infrastructure the team owns to the provider, with no third-party intermediary. Keys, teams and spend logs are stored in the team's own PostgreSQL. Prompt/response logging can be enabled or disabled, so retention is a configuration decision. |
| Policy enforcement | Virtual keys, teams and users with per-key/team budgets, TPM/RPM limits and model allow-lists. Guardrails can be attached through integrations (e.g. Presidio, Lakera) or custom hooks. Some governance features (SSO at scale, audit logs, part of the guardrail and per-team controls) sit behind the enterprise license. |
| Extensibility | MIT-licensed core. Custom callbacks/hooks, custom guardrails, custom auth and custom providers can be written in Python, so behavior can be changed without forking. |
| Deployment and operation | Docker image and Helm chart. Needs PostgreSQL for keys/spend and typically Redis for shared rate limiting across several instances. Being a Python service, throughput and latency overhead need to be checked under load. Release cadence is fast, so upgrades require attention. |
| Provider integrations | Very broad coverage (100+ providers incl. OpenAI, Anthropic, Azure, Bedrock, Vertex, open-model servers) behind a normalized OpenAI-style API. Provider-specific features may be only partly mapped. |
| Configuration isolation | Configured via `config.yaml` and/or the database and admin API. Teams, keys and budgets give logical separation, but all tenants share one proxy process and one model list, so this is not hard multi-tenant isolation. |
| Observability | Built-in spend and request logs plus callbacks to Langfuse, OpenTelemetry, Datadog and others; Prometheus metrics (some behind enterprise). Admin UI shows usage per key/team. |
| Onboarding | Fast: `pip install` or one Docker command, then point an OpenAI client at the proxy. Docs are extensive; the amount of features and options makes production hardening less obvious. |

### Strengths

- Full control over where data flows, because the gateway is self-hosted and open-source.
- Widest provider coverage with one unified API.
- Mature budgeting, rate-limiting and virtual-key model out of the box.
- Hookable in Python, so custom policy logic is easy to add.

### Weaknesses

- Operating it means running and scaling PostgreSQL/Redis and the proxy itself.
- Some governance features (SSO, audit, certain guardrails) require the enterprise license.
- Tenant isolation is logical, not strict; a shared process and config for all teams.
- Python runtime may limit performance at high request rates.

### Evidence

[Research board](YOUR_BOARD_LINK)
