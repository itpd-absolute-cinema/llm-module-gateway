# Gap analysis

A gap is kept only if it passes all four tests:

1. **Somebody needs it.** A named user with a problem, not a feature we find interesting.
2. **The alternatives do not serve it.** Traceable to cells in [comparison.md](comparison.md) and the ALT entries.
3. **It is reachable.** The technique exists; it is engineering, not research.
4. **A team of 3 or 4 could build it in this course.** A minimal slice can be named and tested.

**Evidence status, stated up front.** Tests 2 and 3 rest on the research entries. Tests 1 and 4 currently rest on reasoning, not on a customer conversation or a confirmed course scope. Each gap says so and lists what to check. Test 4 assumes a team of 3 or 4 and a short course timeline; confirm the real schedule before committing.

## Summary

| ID | Gap | Somebody needs it | Not served | Reachable | Buildable in course | Main risk |
|---|---|---|---|---|---|---|
| GAP-01 | Isolated teams on a self-hosted gateway without a paid tier | Plausible, unvalidated | Pass | Pass | Pass (narrow slice) | Incumbents already sell this as enterprise |
| GAP-02 | Audit trail for config changes and request attribution in a free self-hosted gateway | Plausible, unvalidated | Pass for three of four; ALT-04 not observed | Pass | Pass | Cheap for LiteLLM to copy |
| GAP-03 | Self-hosted gateway with per-caller usage views and no external state stores to run | Plausible, unvalidated | Pass | Pass | Pass (narrow slice) | Gives up horizontal scale and high availability |

**Conclusion, not observation:** GAP-01 and GAP-03 are the strongest bases for value propositions; GAP-02 is small and easy to copy, so it is better as part of another proposition than as a proposition alone. GAP-01 and GAP-02 are both governance gaps and may merge if the customer treats them as one need.

---

## GAP-01: Isolated teams on a self-hosted gateway without an enterprise license

**Statement.** A platform team that serves several teams from one self-hosted gateway cannot give each team separated configuration, keys, limits and logs without one of these trade-offs: shared-process logical separation (ALT-01), limited separation without the hosted platform (ALT-02), an enterprise license (ALT-03), or leaving self-hosting (ALT-04).

**User.** Platform engineer in a company where several teams share LLM access and should not see each other's keys, settings or logs.

| Test | Result | Basis |
|---|---|---|
| Somebody needs it | Plausible, **unvalidated** | The user above is our assumption. Comparison row "Configuration isolation" shows the capability is something every product addresses, which suggests demand, but it is not proof. To check: ask 2 or 3 platform engineers whether they have had to separate teams on a shared gateway, and what they did. |
| Alternatives do not serve it | **Pass** for self-hosted, non-paid use | ALT-01: teams, keys and budgets give logical separation, but one process and one model list are shared. ALT-02: isolation is limited on a self-hosted gateway alone. ALT-03: workspaces and RBAC are enterprise or Konnect. ALT-04: per-gateway separation exists but only hosted. |
| Reachable | **Pass** | Per-tenant keys, configuration and log stores enforced at the request entry point is ordinary engineering; ALT-03 already does a fine-grained version. |
| Buildable in course | **Pass for a narrow slice** | Slice: two or more tenants, each with its own keys, config and log store, plus an automated test showing tenant A cannot read or change tenant B's config or logs. Excluded: SSO, guardrails, UI. |

**Scope honesty.** This gap is "without a paid tier", not "stronger than the paid tier". If a customer already pays for Kong enterprise or LiteLLM enterprise, this gap does not apply to them.

---

## GAP-02: Audit trail for config changes and request attribution in a free self-hosted gateway

**Statement.** When something goes wrong (a policy was changed, a cost spike appeared), a team needs to answer who changed what and which caller made which requests. In ALT-01 audit logs are enterprise-licensed, ALT-03 ties RBAC and workspaces to enterprise or Konnect, and ALT-02 places advanced governance in the hosted or enterprise platform. ALT-04 was not observed for audit, so this gap is unverified there.

**User.** Engineer or security owner in a company that must explain gateway changes and usage after an incident or to an auditor.

| Test | Result | Basis |
|---|---|---|
| Somebody needs it | Plausible, **unvalidated** | To check: ask whether anyone has had to reconstruct who changed a gateway setting, and how they did it today. |
| Alternatives do not serve it | **Pass for ALT-01, ALT-02, ALT-03; not verified for ALT-04** | See statement. Before relying on this gap, read ALT-04's documentation on audit and add it to the entry. |
| Reachable | **Pass** | Append-only structured logs of administrative actions and request metadata are a standard technique. |
| Buildable in course | **Pass** | Slice: append-only log of config and key changes with actor and timestamp, per-key request attribution, and an export. Excluded: SSO, tamper-evident storage. |

**Risk.** An audit log is a small feature. If LiteLLM moved it out of the enterprise tier, it would take a release, so this is not a durable advantage on its own.

---

## GAP-03: Self-hosted gateway with per-caller usage views and no external state stores to run

**Statement.** The products that keep data in the team's own infrastructure with usage views per caller need PostgreSQL and typically Redis (ALT-01) or a database or DB-less mode plus a control and data plane (ALT-03). The light self-hosted option (ALT-02's gateway) lacks dashboards and broad governance without the hosted platform. The product with the fastest start (ALT-04) sends traffic through a third party. A team that wants control and little operations work has no matching option among the four.

**User.** Small team or lead developer who must keep prompts inside the company but has no capacity to run a database cluster and a cache.

| Test | Result | Basis |
|---|---|---|
| Somebody needs it | Plausible, **unvalidated** | ALT-04's onboarding (minutes, free core features) shows how fast the hosted route is, which is the standard we would be compared against. To check: ask teams that chose a hosted gateway whether data control was the reason they did not self-host. |
| Alternatives do not serve it | **Pass** | See statement; comparison rows "Data control", "Deployment and operation", "Onboarding". |
| Reachable | **Pass** | A single process with embedded storage (for example SQLite) and a per-key usage view is a known design. |
| Buildable in course | **Pass for a narrow slice** | Slice: OpenAI-compatible proxy to two or three providers, embedded storage, per-key usage view, run with one command. Excluded: horizontal scaling, high availability. |

**What it gives up.** A single-node design does not scale out and has no high availability. That is a real limit for high-traffic teams and must be stated to the customer.

---

## Rejected gaps

Each of these was considered and dropped. The last column says what would change our mind, so the customer can argue with the specific reason.

| ID | Candidate gap | Failed test | Why rejected | What would make us reconsider |
|---|---|---|---|---|
| REJ-01 | Wider provider coverage | 2 | ALT-01 covers 100+ providers and ALT-02 200+ LLMs; ALT-03 and ALT-04 cover the major ones. Breadth is already served. | The customer uses a provider that none of the four supports. |
| REJ-02 | Reliability routing (fallbacks, retries, load balancing, caching) | 2 | ALT-01 (fallbacks, load balancing), ALT-02 (fallbacks, retries, load balancing, caching) and ALT-04 (caching, retries, fallbacks) already provide it. | A pilot shows failover behavior in one of them that does not meet the customer's needs. |
| REJ-03 | Extension or plugin system | 2 | ALT-01 (Python hooks, custom guardrails) and ALT-03 (plugins in Lua, Go, JavaScript, Python) already provide it. Only ALT-04 lacks it, and Workers partly fill that. | The customer cannot express a needed rule in either hook system. |
| REJ-04 | Hosted dashboard with no operations work | 2 | ALT-04 already offers a built-in dashboard with logs and cost, with core features free. A student team cannot out-compete a vendor feature there. | The customer rejects ALT-04 for reasons other than data control. |
| REJ-05 | Higher throughput than a Python gateway | 1 | The only evidence is that throughput must be checked under load (ALT-01). No measurement shows a problem. The need is unvalidated. | A benchmark shows ALT-01's overhead matters at the customer's request rate. |
| REJ-06 | Better guardrail detection (PII, prompt injection) | 2 and 4 | ALT-01 (integrations), ALT-02 (guardrails), ALT-03 (prompt guard) and ALT-04 (guardrails, DLP) all provide some. Building detection models that beat them is beyond a small team in a course. | The customer supplies its own detector and needs only a hook, which REJ-03 says already exists. |
| REJ-07 | Hard isolation guarantees (separate runtimes per tenant with a verified boundary) | 4 | This is a stronger version of GAP-01. Verified isolation is too large a build and verification effort for the course; GAP-01 is deliberately limited to enforced, tested logical isolation. | The customer requires a compliance-grade boundary and accepts a longer schedule. |
| REJ-08 | SSO and identity integration | 4, and weak differentiation | ALT-01 keeps SSO at scale in the enterprise tier, but ALT-03 already supports key, JWT and OIDC authentication. It is integration-heavy, and adds little beyond GAP-01 and GAP-02. | The customer makes SSO a hard requirement for the pilot. |

---

## Before building on these gaps

- Ask two or three potential users the "to check" question in GAP-01, GAP-02 and GAP-03. All three "somebody needs it" results are currently assumptions.
- Verify audit and identity capabilities in ALT-04 and update its entry.
- Recheck enterprise-only and hosted-only claims against the versions recorded in the ALT entries, since these change often.
- Confirm the course timeline and team size; the "narrow slice" in each gap is sized for a team of 3 or 4.
