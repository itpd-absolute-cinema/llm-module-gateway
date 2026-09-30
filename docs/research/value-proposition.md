# Value proposition

Two value propositions, built on the gaps in [gap-analysis.md](gap-analysis.md). GAP-02 (audit trail) is folded into VP-01 because, as noted there, it is too small and too easy to copy to stand alone.

Claims about alternatives cite the ALT entries ([ALT-01 LiteLLM](alternatives/litellm.md), [ALT-02 Portkey](alternatives/portkey.md), [ALT-03 Kong AI Gateway](alternatives/kong-ai-gateway.md), [ALT-04 Cloudflare AI Gateway](alternatives/cloudflare-ai-gateway.md)). Statements about what a competitor could do are **inferences** and are labeled as such. Nothing here is a claim about any vendor's roadmap or business.

---

## VP-01: Separate teams on one self-hosted gateway, without an enterprise license

**User:** platform engineer who serves several teams from one self-hosted LLM gateway.

**Problem:** separating teams is either logical only, inside one shared process and model list (ALT-01), limited without the hosted platform (ALT-02), tied to enterprise workspaces and RBAC (ALT-03), or available only as a hosted service (ALT-04). A record of who changed which setting is enterprise-licensed in ALT-01 and was not observed in ALT-04.

**What we do that the alternatives do not:** give each team its own keys, configuration and log store in a self-hosted deployment, and keep an append-only record of who changed what and which key made which requests, with no paid tier.

**How the claim can be checked:**
- An automated test shows that a key from team A cannot read or change team B's configuration or logs.
- Every configuration or key change appears in an exportable record with actor and timestamp.

**Closes:** [GAP-01](gap-analysis.md#gap-01-isolated-teams-on-a-self-hosted-gateway-without-an-enterprise-license), [GAP-02](gap-analysis.md#gap-02-audit-trail-for-config-changes-and-request-attribution-in-a-free-self-hosted-gateway).

**What it costs:**
- Isolation is logical and enforced by tests, not a verified hard boundary (REJ-07), so it does not suit a customer whose compliance requires one.
- No SSO (REJ-08) and no guardrails of our own (REJ-06).
- A shorter provider list than ALT-01 and ALT-02 (REJ-01 records that breadth is already served by them).
- Each team has to be onboarded as a tenant, which is more setup than one shared configuration.

**How a competitor would respond:** *Inference.* ALT-01 and ALT-03 already have these capabilities behind their enterprise licenses, so the technical work exists on their side; we cannot observe whether they would offer it in a free tier. A customer who already holds those licenses is not our user. We do not claim a moat. What remains defensible is the combination with VP-02: isolation and audit that run from one command with nothing else to operate.

**Confidence:** moderate that the gap exists (four ALT entries agree for self-hosted, non-paid use; ALT-04 audit was not observed). Low that anyone needs it, because no user has been asked yet (Assumptions 1 to 4).

---

## VP-02: A self-hosted gateway that runs from one command, with no database or cache to operate

**User:** small team or lead developer who must keep prompts inside the company and has no capacity to run PostgreSQL, Redis or a control plane.

**Problem:** the self-hosted options with usage views need PostgreSQL and typically Redis (ALT-01), or a database or DB-less mode plus a control and data plane (ALT-03). The light self-hosted gateway (ALT-02) lacks dashboards and broad governance without the hosted platform. The fastest start, a gateway created in minutes (ALT-04), sends all traffic through a third party.

**What we do that the alternatives do not:** run as one process with embedded storage, and show usage per key, so prompts stay in the team's infrastructure without operating any other service.

**How the claim can be checked (proposed targets, not yet measured):**
- Number of external services needed to run: 0.
- Time from a clean machine to the first successful proxied request: 10 minutes or less, following only our README. The baseline for ALT-01 and ALT-04 on the same measure has not been taken (Assumption 8).
- A per-key usage view is available without installing anything else.

**Closes:** [GAP-03](gap-analysis.md#gap-03-self-hosted-gateway-with-per-caller-usage-views-and-no-external-state-stores-to-run).

**What it costs:**
- No horizontal scaling and no high availability: if the node stops, the gateway stops. That matters for teams whose production traffic cannot tolerate a pause, and is acceptable mainly for internal or development use.
- Embedded storage limits log volume and retention.
- Backup of the node is the team's job; data control is only as good as it.
- Fewer providers than ALT-01 and ALT-02.

**How a competitor would respond:** *Inference.* ALT-02's gateway is already light, so adding a local usage view is an incremental step for it; ALT-01 would need an additional storage mode besides PostgreSQL. Either could close the gap with moderate effort. Our only durable position is to measure and publish the two numbers above and to stay narrower than they are: single-node, few providers, no scale-out.

**Confidence:** moderate that the gap exists (comparison rows "Data control", "Deployment and operation", "Onboarding"). Low that the PostgreSQL and Redis requirement actually stops the target user, since it has not been tested (Assumptions 7 to 9).

---

## How the two fit together

Both describe one product: a single-node, self-hosted gateway where each team has separate keys, configuration and logs, started with one command. The costs add up: logical isolation only, no high availability, few providers. If design work shows that per-team separation cannot be done with embedded storage in one process (Assumption 11), VP-01 and VP-02 pull apart and one of them has to go.

## Won't Have check

No Won't Have list exists in the research files yet, so the required conflict check cannot be completed. Proposed Won't Have items that follow from the costs above: verified hard isolation (REJ-07), SSO (REJ-08), own detection models for guardrails (REJ-06), horizontal scaling and high availability. Add them to the Won't Have list when it is written, and recheck VP-01 and VP-02 against it.

---

## Assumptions

Beliefs the proposal rests on that have not been verified. "Customer" means the belief cannot be settled by the team alone and belongs in the meeting report's open questions.

| # | Assumption | Supports | How to check | When | Settled by |
|---|---|---|---|---|---|
| 1 | Platform engineers who share one self-hosted gateway across several teams exist in the target customer and are blocked by shared-process separation. | GAP-01, VP-01 | Ask the customer and 2 or 3 platform engineers whether they have had to separate teams on a gateway, and what they did. | Customer meeting; before the Week 2 scope proposal | Customer |
| 2 | Logical isolation enforced by tests is enough for the target customer; a verified hard boundary is not required. | GAP-01, VP-01 | Ask what boundary their compliance or security rules require. | Customer meeting | Customer |
| 3 | The target customer does not already hold the enterprise licenses of ALT-01 or ALT-03. | GAP-01, GAP-02, VP-01 | Ask which gateway, if any, they run and on which license. | Customer meeting | Customer |
| 4 | Someone needs an audit record of configuration changes and per-key request attribution. | GAP-02, VP-01 | Ask whether anyone has had to reconstruct who changed a gateway setting and how they did it. | Customer meeting | Customer |
| 5 | ALT-04 offers no audit or identity features comparable to GAP-02. | GAP-02, VP-01 | Read ALT-04's documentation on audit logging and update its entry. | Now, in Week 1 | Team |
| 6 | The enterprise-only and hosted-only claims in the ALT entries are current for the recorded versions. | GAP-01, GAP-02, VP-01 | Recheck each against the vendor's documentation at the version recorded in its ALT entry. | Before the Week 2 scope proposal | Team |
| 7 | Teams that choose a hosted gateway over self-hosting do so because of operating effort, not only cost. | GAP-03, VP-02 | Ask the customer and any users of hosted gateways why they chose hosting. | Customer meeting | Customer |
| 8 | Standing up ALT-01 with its documentation takes substantially longer than 10 minutes and needs several services, while ALT-04 takes minutes. The baseline has not been measured. | GAP-03, VP-02 | Time a clean-machine setup of ALT-01 and ALT-04 to first proxied request; record services required. | Week 2 | Team |
| 9 | One node with embedded storage carries the target team's request rate and log retention. | GAP-03, VP-02 | Ask the customer for expected request rate and retention; load-test the prototype. | Customer meeting; again at the Week 5 release | Customer and team |
| 10 | The target customer uses only a few providers, all of which we can support. | VP-01, VP-02 | Ask which providers and models they call today. | Customer meeting | Customer |
| 11 | Per-team separation can be built with embedded storage in a single process. | VP-01, VP-02 | Short design spike: two tenants, separate stores, one process, isolation test passing. | Weeks 2 and 3, before the MVP scope is fixed | Team |
| 12 | A team of 3 or 4 can build the slices named in GAP-01, GAP-02 and GAP-03 within the course. | GAP-01, GAP-02, GAP-03, VP-01, VP-02 | Estimate each slice for the Week 2 work plan against the actual schedule. | Week 2 | Team |
