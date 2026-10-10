# ALT-03

Kong AI Gateway

- **Status:** Active
- **Kind:** General-purpose API gateway extended with AI plugins (open-source core + enterprise/SaaS)
- **Link:** [https://github.com/Kong/kong](https://github.com/Kong/kong)
- **Version looked at:** Not recorded in Week 1.
- **Depth of evaluation:** Documentation, repository, architecture, configuration and usage examples.
- **Problem it solves:** Kong AI Gateway applies the existing Kong API gateway to LLM traffic through a set of AI plugins (AI Proxy, prompt guard/decorator/template, semantic cache, AI rate limiting, request/response transformers).
  It lets organizations that already run Kong manage LLM access with the same authentication, rate limiting, logging and deployment model as the rest of their APIs, instead of adding a separate LLM-specific product.

**Observations by property**

| Property | Observation |
| --- | --- |
| Data control | Self-managed (VMs, Kubernetes, hybrid mode with separate control and data planes) or managed through Konnect. In self-managed deployments, prompts stay inside the organization's infrastructure; with Konnect only control-plane metadata is managed by Kong. |
| Policy enforcement | Strongest among the alternatives for generic API policy: authentication (key, JWT, OIDC), ACLs, rate limiting, IP restrictions, plus AI-specific prompt guard, prompt templates and token-based rate limiting. Several "Advanced" AI plugins (e.g. AI Proxy Advanced, semantic features, advanced rate limiting) require an enterprise license. |
| Extensibility | Large plugin ecosystem; custom plugins can be written in Lua, Go, JavaScript or Python. Plugins run in the request path, so any check or transformation can be added. |
| Deployment and operation | Built on Nginx/OpenResty and proven at scale. Declarative configuration (decK, Kubernetes CRDs/Ingress Controller) fits GitOps. Operating Kong (database or DB-less mode, control/data plane) is heavier than a single-purpose LLM proxy. |
| Provider integrations | Supports major providers (OpenAI, Azure OpenAI, Anthropic, Bedrock, Gemini, Mistral, Cohere, self-hosted models) through AI Proxy with a normalized OpenAI-style format. The list is shorter than LiteLLM or Portkey. |
| Configuration isolation | Workspaces (enterprise), services, routes, consumers and plugin scoping give fine-grained, per-route and per-consumer isolation, with RBAC in the enterprise/Konnect tiers. |
| Observability | Prometheus, OpenTelemetry, Datadog, Splunk and log-shipping plugins. AI plugins emit token usage and model metadata, but there are no built-in LLM-specific dashboards like prompt browsing or per-key spend views. |
| Onboarding | Easy for teams that already know Kong; steeper for others because of the concepts (services, routes, consumers, plugins) and the OSS vs enterprise plugin split. |

**Strengths**

- One gateway for both regular APIs and LLM traffic, with consistent policies.
- Mature authentication, authorization, rate limiting and multi-tenant controls.
- Powerful plugin system and GitOps-friendly declarative configuration.
- Proven performance and operational tooling at scale.

**Weaknesses**

- Advanced AI features and workspace/RBAC capabilities require paid tiers.
- Heavier to deploy and operate than LLM-specific proxies.
- Fewer providers and less LLM-native tooling (cost tracking, budgets per key, prompt management) out of the box.
- Steeper learning curve for teams without Kong experience.
