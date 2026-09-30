# Comparison of the alternatives

## How to read this document

The first column states the question asked of every product and the condition under which the answer matters, because a weakness is only a weakness for some use. "Not observed" means the ALT entry does not cover it; it does not mean the product lacks it.

Sources:
[ALT-01 LiteLLM](alternatives/litellm.md) · [ALT-02 Portkey](alternatives/portkey.md) · [ALT-03 Kong AI Gateway](alternatives/kong-ai-gateway.md) · [ALT-04 Cloudflare AI Gateway](alternatives/cloudflare-ai-gateway.md)

## Table

| Property: question, and when it matters | ALT-01 LiteLLM | ALT-02 Portkey | ALT-03 Kong AI Gateway | ALT-04 Cloudflare AI Gateway |
|---|---|---|---|---|
| **Data control.** Can prompts and logs stay entirely inside the team's own infrastructure? Matters when prompts contain source code or customer data. | **Yes.** Self-hosted; keys and spend logs in the team's own PostgreSQL; prompt logging is a setting (ALT-01). | **Partial.** The open-source gateway can run in the team's environment, but the hosted platform passes requests and logs through Portkey; private or hybrid deployment is an enterprise offering (ALT-02). | **Yes, when self-managed.** Hybrid mode keeps prompts in the organization's infrastructure; with Konnect only control-plane metadata is managed by Kong (ALT-03). | **No.** All traffic passes through Cloudflare; retention and logging are configurable, but there is no self-hosted option (ALT-04). |
| **Policy enforcement.** Can different callers get different limits, budgets and content rules? Matters when several teams or apps share one gateway. | **Yes.** Per-key and per-team budgets, TPM/RPM limits, model allow-lists, guardrail integrations. SSO at scale, audit logs and part of the guardrail and per-team controls are enterprise-licensed (ALT-01). | **Yes, mostly hosted.** Declarative configs per request or key for routing, retries, caching and guardrails; budgets and rate limits. Advanced governance mostly sits in the hosted or enterprise platform (ALT-02). | **Yes.** Key/JWT/OIDC auth, ACLs, rate limiting, prompt guard, token-based rate limiting. The "Advanced" AI plugins need an enterprise license (ALT-03). | **Partial.** Gateway-level rate limiting, caching, retries, fallbacks, optional authentication, guardrails/DLP. No per-user virtual keys with budgets (ALT-04). |
| **Extensibility.** Can the team add its own check or transformation without forking? Matters when policy is specific to the company. | **Yes.** MIT core; Python callbacks, custom guardrails, custom auth, custom providers (ALT-01). | **Partial.** Provider and guardrail plugins in the open-source gateway; custom guardrails through webhooks; narrower than a general plugin system (ALT-02). | **Yes.** Large plugin ecosystem; custom plugins in Lua, Go, JavaScript or Python, running in the request path (ALT-03). | **No, inside the gateway.** No plugin mechanism; custom logic goes into Workers placed in front of or behind it, or into metadata and headers (ALT-04). |
| **Deployment and operation.** What must the team run and scale? Matters when the team has little operations capacity. | Docker and Helm; needs PostgreSQL and typically Redis for several instances; Python throughput must be checked under load (ALT-01). | Small TypeScript gateway (Node.js, Docker, edge runtimes); self-hosting gives the proxy only, dashboards depend on the platform (ALT-02). | Nginx/OpenResty base, proven at scale, GitOps-friendly (decK, CRDs); database or DB-less mode and control/data plane make it heavier than an LLM-only proxy (ALT-03). | Nothing to deploy; runs on Cloudflare's network; availability, limits and roadmap belong to the vendor (ALT-04). |
| **Provider integrations.** How many providers behind one API? Matters when models are switched or mixed often. | 100+ providers behind an OpenAI-style API; provider-specific features may be only partly mapped (ALT-01). | 200+ LLMs and several modalities (ALT-02). | Major providers (OpenAI, Azure OpenAI, Anthropic, Bedrock, Gemini, Mistral, Cohere, self-hosted); shorter list than ALT-01 and ALT-02 (ALT-03). | Major hosted providers; no arbitrary self-hosted backends beyond what the endpoints allow (ALT-04). |
| **Configuration isolation.** Can one team's keys, limits and settings be separated from another's? Matters in multi-team or multi-tenant use. | **Logical only.** Teams, keys and budgets separate users, but all share one proxy process and one model list (ALT-01). | **Partial.** Workspaces, API keys, virtual keys and versioned configs in the platform; on a self-hosted gateway alone, isolation is limited (ALT-02). | **Yes, at fine grain.** Services, routes, consumers and per-route plugin scoping; workspaces and RBAC are enterprise or Konnect (ALT-03). | **Yes, per gateway.** Separate gateways per application or environment, each with its own settings, logs and optional authentication; scoped by Cloudflare account roles (ALT-04). |
| **Observability.** Can the team see usage, cost and errors per caller? Matters when cost or incidents must be attributed. | Spend and request logs; callbacks to Langfuse, OpenTelemetry, Datadog; Prometheus (some enterprise); admin UI per key and team (ALT-01). | Logs, cost, latency, error analytics, traces and feedback in the hosted platform; the open-source gateway alone has basic logging (ALT-02). | Prometheus, OpenTelemetry, Datadog, Splunk and log-shipping plugins; AI plugins emit token usage; no built-in LLM-specific dashboards (ALT-03). | Built-in dashboard (requests, tokens, cost, errors, cache hits), searchable logs, Logpush export, custom metadata (ALT-04). |
| **Onboarding.** How fast to a first working request? Matters when the evaluation or pilot has a short time box. | `pip install` or one Docker command, then point an OpenAI client at it; many options make production hardening less obvious (ALT-01). | Change the base URL and add a header or config ID; the OSS-versus-hosted split needs extra reading (ALT-02). | Easy for teams that already know Kong; steeper otherwise (services, routes, consumers, plugins, OSS/enterprise split) (ALT-03). | Minutes: create a gateway and change the base URL; core features on the free plan, with log volume limits (ALT-04). |

## Reading the table as a whole

Everything below is **conclusion**, drawn from the table above. Row names point back to the evidence.

### Down the columns: how strong is the strongest product?

- **ALT-01 LiteLLM** is the strongest LLM-specific option on the rows that concern control: data control, policy enforcement, extensibility and provider breadth are all strong. Its weak rows are configuration isolation (logical only) and operation (PostgreSQL, Redis, Python throughput). It is the bar for anything positioned as an LLM gateway.
- **ALT-03 Kong** is the strongest on policy depth and isolation, but only with enterprise features, and it is the heaviest to operate and has the shortest provider list of the self-hostable options. It is the bar for teams that already run an API gateway.
- **ALT-04 Cloudflare** is the strongest on deployment and onboarding and the weakest on data control and extensibility.
- **ALT-02 Portkey** depends on which half is used: the open-source gateway is light but limited; the platform is complete but hosted.

### Across the rows: where is every product weak or absent?

- **Configuration isolation:** no product gives strong isolation in a self-hosted, non-paid setup. ALT-01 is logical only, ALT-02 is limited without the platform, ALT-03 needs enterprise workspaces and RBAC, ALT-04 is hosted only.
- **Governance and analytics are tied to a commercial tier or hosted service** in every product where they were observed: ALT-01 (SSO, audit logs), ALT-02 (advanced governance, analytics), ALT-03 (Advanced AI plugins, workspaces, RBAC).
- **Operation versus control:** the rows that favor the team (data control, extensibility) and the rows that favor convenience (deployment, onboarding) are won by different products.

### The diagonal

No product is strong on every row, so there is no single incumbent to beat. The closest to a diagonal on control is ALT-01, and on ease ALT-04, and each loses on the other axis. The bar is therefore two-sided: match ALT-01 on control and ALT-04 on time to first request.

### Candidates

Written down before judging any of them. Each can be disputed by pointing at a cell above or at a product not in the table.

1. **No alternative offers hard tenant isolation in a self-hosted deployment without a paid tier.** Rows: Configuration isolation, Deployment. Evidence: ALT-01, ALT-02, ALT-03, ALT-04. Would be disproved by a free, self-hosted setup of any of the four that separates tenants beyond keys and budgets.
2. **Among these four, full self-hosted governance and observability requires operating state stores, while the lightweight option gives them up.** ALT-01 and ALT-03 need PostgreSQL/Redis or a control plane; ALT-02's small self-hosted gateway lacks dashboards and broad governance without the platform. Would be disproved by a self-hosted product that has per-caller usage views and policy with no database to run.
3. **In the self-hosted free tier, per-caller budgets exist only in LiteLLM, and audit logs and SSO are not available in any of the four.** Rows: Policy enforcement. Evidence: ALT-01 (audit and SSO enterprise), ALT-02 and ALT-03 (governance and RBAC enterprise or hosted). Caveat: ALT-04 was not observed for audit or SSO; verify before relying on this.
4. **The products that win on data control (ALT-01, ALT-03) lose on time to first request, and the one that wins on time to first request (ALT-04) loses on data control.** Rows: Data control, Deployment, Onboarding. Would be disproved by evidence that a self-hosted option onboards as fast as ALT-04 once production hardening is included.

## Limits of this table

- Cells rest on the ALT entries, and those entries have placeholders for version and date researched. Claims about which features are enterprise-only or hosted-only change quickly and should be rechecked against the version recorded there.
- "Partial" means the ALT entry shows the capability exists with a stated limit; the limit is given in the cell.
