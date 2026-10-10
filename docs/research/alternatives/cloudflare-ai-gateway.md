# ALT-04

Cloudflare AI Gateway

- **Status:** Active
- **Kind:** Managed (SaaS) AI gateway
- **Link:** [https://developers.cloudflare.com/ai-gateway/](https://developers.cloudflare.com/ai-gateway/)
- **Version looked at:** Not recorded in Week 1.
- **Depth of evaluation:** Documentation, architecture, configuration and usage examples (closed source, no public repository).
- **Problem it solves:** Cloudflare AI Gateway is a proxy hosted on Cloudflare's network that sits between an application and LLM providers.
  It adds analytics, logging, caching, rate limiting, retries and model fallback with minimal setup, usually by changing the provider base URL.
  It is aimed at teams that want visibility and basic control over LLM usage without running any gateway infrastructure.

**Observations by property**

| Property | Observation |
| --- | --- |
| Data control | All traffic passes through Cloudflare, which is a third-party intermediary in addition to the model provider. Log retention is configurable and logging can be disabled per gateway, but the data path cannot be moved into the team's own infrastructure. |
| Policy enforcement | Per-gateway rate limiting, caching rules, retries and fallbacks, optional gateway authentication, and guardrails/DLP features. Policy model is gateway-level; there are no per-user virtual keys with budgets comparable to LiteLLM or Portkey. |
| Extensibility | Closed platform with no custom plugin mechanism in the gateway itself. Custom logic can be added by placing Cloudflare Workers in front of or behind it, or by using custom metadata and headers. |
| Deployment and operation | Nothing to deploy or scale; runs on Cloudflare's global network and is configured via dashboard, API or Terraform. Availability, limits and feature roadmap depend entirely on the vendor. |
| Provider integrations | Supports major hosted providers (OpenAI, Anthropic, Azure OpenAI, Bedrock, Google, Mistral, Workers AI and others) through provider-specific and unified endpoints. No support for arbitrary self-hosted backends beyond what the endpoints allow. |
| Configuration isolation | Separate gateways per application or environment, each with its own settings, logs and (optionally) authentication. Scoped by Cloudflare account and its role-based access. |
| Observability | Built-in dashboard with requests, tokens, cost, errors and cache hit rate, plus searchable logs; data can be exported through Logpush and custom metadata for filtering. |
| Onboarding | Fastest of the four: a gateway can be created in minutes and used by changing the base URL. Core features are available on the free plan; log volume limits apply. |

**Strengths**

- No infrastructure to run; very quick to adopt.
- Useful caching, rate limiting and analytics out of the box, with low added latency from edge routing.
- Natural fit if the stack already uses Cloudflare (Workers, Workers AI).
- Low or no cost for core features.

**Weaknesses**

- Data must flow through Cloudflare; no self-hosted option.
- Limited extensibility and governance (no per-user keys or budgets, no custom plugins).
- Vendor lock-in and dependence on vendor limits and roadmap.
- Fewer options for deep policy enforcement than the self-hosted alternatives.
