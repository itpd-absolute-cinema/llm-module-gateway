# ALT-02

Portkey

- **Status:** Active
- **Kind:** AI gateway with a hosted control plane (open-source gateway core + commercial platform)
- **Link:** [https://github.com/Portkey-AI/gateway](https://github.com/Portkey-AI/gateway)
- **Version looked at:** Not recorded in Week 1.
- **Depth of evaluation:** Documentation, repository, architecture, configuration and usage examples.
- **Problem it solves:** Portkey gives applications one API to many LLM providers and adds reliability and governance features on top: fallbacks, retries, load balancing, caching, guardrails, budgets and request logging.
  Its lightweight TypeScript gateway can be self-hosted, while the hosted platform adds prompt management, observability dashboards and access control.
  It targets teams that want production controls for LLM traffic without building them.

**Observations by property**

| Property | Observation |
| --- | --- |
| Data control | Two modes: the open-source gateway run in the team's own environment, or the hosted SaaS where requests and logs pass through Portkey. Enterprise plans offer private/hybrid deployment. Which mode is used decides who sees prompts and logs. |
| Policy enforcement | Declarative "configs" define routing, retries, fallbacks, caching and guardrails per request or per key. Guardrails (input/output checks, PII, custom webhooks) and budget/rate limits are available; the more advanced governance features mostly live in the hosted/enterprise platform. |
| Extensibility | Provider and guardrail plugins can be added to the open-source gateway, and custom guardrails can be called through webhooks. Extension points are narrower than a general plugin system. |
| Deployment and operation | The gateway is small (TypeScript, runs on Node.js, Docker or edge runtimes such as Cloudflare Workers) and has a low footprint. Self-hosting gives the proxy only; dashboards, prompt management and full log analytics depend on the platform. |
| Provider integrations | 200+ LLMs and several modalities via a unified OpenAI-style API, with SDKs and OpenAI-client compatibility. |
| Configuration isolation | Workspaces, API keys and virtual keys separate teams and projects in the platform; configs can be versioned and attached per key. Isolation on a self-hosted gateway alone is limited. |
| Observability | Request logs, cost, latency and error analytics, traces and feedback in the hosted platform; the OSS gateway alone offers basic logging. |
| Onboarding | Quick: change the base URL and add a header or config ID. Good docs and SDKs; understanding which features are OSS and which are hosted takes extra reading. |

**Strengths**

- Lightweight gateway that is easy to embed or self-host.
- Strong reliability tooling (fallbacks, retries, load balancing, caching) driven by simple declarative configs.
- Integrated guardrails, prompt management and observability in one product.
- Very wide provider and model coverage.

**Weaknesses**

- Full feature set (analytics, governance, prompt management) depends on the hosted platform or enterprise tier, which affects data control.
- Self-hosted gateway alone has limited isolation and observability.
- Smaller community and fewer extension points than a general-purpose API gateway.
