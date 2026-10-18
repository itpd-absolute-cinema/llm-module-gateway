# Meeting Report: Week 08 Customer Sync

## Metadata

- **Date:** 2026-10-16
- **Duration:** ~45 minutes
- **Present:** Customer, Artem Lisitcin
- **Presented:** LLM Gateway architecture diagrams (client side, provider side, HTTP API, request core, plugin runner, provider adapter, request/response plugin chains), request lifecycle and plugin chain concept, prototype Swagger page with mock provider request log, plugin constructor/interface code (`gateway7plugins` Python file), YAML configuration example showing provider settings and plugin list (name, enabled, required, request/response chains)
- **Permission to record:** Yes — Denis started recording at the beginning ("I also started recording, just in case")
- **Permission to publish transcript:** Yes — Denis stated "I'll post the transcript" and noted "Since it's gonna be a public transcript, I won't name any names"
- **Transcript:** [GMT20261016-051701_Recording.transcript.txt](../week-03/meeting-transcript.md)
- **Meeting script:** [reports/week-03/meeting-script.md](../week-03/meeting-script.md)

## Previous action points

- "Prepare architecture diagrams and a working prototype demonstrating the plugin chain" — Carried out. The team presented the LLM Gateway architecture diagrams (client/provider sides, request core, plugin runner, provider adapter) and a working prototype: a Python/FastAPI app with a Swagger page sending a request through a Mask plugin to a mock provider, visible in the provider's request log with the phone number masked. The customer found the prototype functional but noted one plugin is trivial and asked for at least three. Decision: None.

- "Define the plugin interface and configuration format for the gateway" — Carried out. The team showed the plugin constructor interface (ID, API version, settings model, on_request/on_response hooks) and the YAML configuration (provider, plugin list with name/enabled/required, request and response chains, ordering). The customer confirmed the configuration model is the kind of interface he would use, and asked that the plugin interface requirements be documented so a developer can add a plugin quickly. Decision: None.

- "Clarify the target audience and the problem the gateway solves for them" — Carried out. The customer restated the target audience as a DevOps engineer who must use LLMs responsibly under IT and security requirements, needs to write plugins quickly, and must be able to identify which plugin failed and where. This confirmed and sharpened the problem statement carried into this meeting. Decision: None.

## Previous open questions

- "What level of plugin isolation and failure handling does the customer expect?" — Partially answered. The customer expects the system to identify which plugin failed and where, with per-plugin timing/logging, and to handle plugin exceptions deliberately (propagate, wrap, or degrade). He did not pick a specific policy, leaving fail-fast vs graceful degradation vs configurable per-plugin open. Decision: None.

- "How many plugins must the MVP ship with, and which behaviors?" — Answered. The customer requires at least three plugins for a minimum usable application; one plugin is trivial. The specific set of behaviors remains open (PII masking is present; logging and content filtering were suggested but not confirmed). Decision: None.

- "Should the MVP connect to a real LLM provider or is a mock acceptable?" — Answered. The customer said a mock provider is acceptable for this milestone: "it's not about the actual LLM," but connecting to a real provider would be great if possible. Decision: None.

## Summary

- The customer reviewed the LLM Gateway architecture and found the high-level diagram too trivial: the plugin runner is a black box with no answer for failure detection, monitoring, timing, or error propagation.
- The customer pushed for at least three plugins in the MVP (one plugin is "not a real application"), and wants the ability to add a plugin himself by following documentation.
- The customer wants a Minimum Usable Product by end of this week: downloadable, startable, able to send prompts through plugins with visible transformations, and extensible with a new plugin quickly.
- The customer recommended studying Django's middleware system and Twisted's callback/error interface as architectural references for the plugin chain design.
- The customer clarified the target audience: a DevOps engineer who must use LLMs responsibly under security/IT requirements, needs to write plugins quickly, and must be able to identify which plugin failed and where.

## Decisions

- It was agreed with the customer to deliver a Minimum Usable Product by next week, containing at least three plugins, setup documentation, and a documented way to add a new plugin.

## Action points

| Action | Owner | Due |
| -------- | ------- | ----- |
| Deliver MUP by end of the next week: runnable app with at least 3 plugins, documented plugin addition, visible request/response transformations | Artem Lisitcin | Week 08 |
| Study Django middleware system and Twisted callback/error interface; document chosen plugin-chain architecture with rationale | Artem Lisitcin | Week 09 |
| Document plugin interface requirements (constructor, ID, API version, settings model, on_request/on_response hooks) so a developer can add a plugin quickly | Artem Lisitcin | Week 09 |
| Add per-plugin timing/logging so the customer can see which plugin ran, in what order, and where it failed | Artem Lisitcin | Week 09 |
| Define and implement failure-handling policy (fail-fast vs graceful degradation vs configurable per-plugin) | Artem Lisitcin | Week 09 |

## Open questions

| Question | What it would change | Follow-up |
| ---------- | --------------------- | ----------- |
| Which failure-handling policy does the customer prefer: fail-fast, graceful degradation, or configurable per-plugin? | Determines the plugin runner's error-handling implementation and whether `required` flag in YAML becomes the controlling mechanism | Ask at next meeting after presenting a concrete proposal with trade-offs |
| Should errors propagate back through previously executed plugins (for cleanup/transformation) or return directly to the client with a plugin identifier? | Changes the plugin interface (whether plugins need an error hook) and the request core's error path | Ask at next meeting with a concrete interface proposal |
| Is the current plugin interface (separate `on_request`/`on_response` hooks) acceptable, or does the customer need a combined plugin with shared state (Django middleware-style)? | Changes the plugin base class and all existing plugin implementations | Ask at next meeting after studying Django middleware |
| What is the minimum set of plugins and behaviors (e.g., PII masking, logging, content filtering) required for the MVP to be considered usable? | Defines the MVP plugin library scope and acceptance criteria | Confirm at next meeting with a proposed list |
| Should the MVP connect to a real LLM provider or is a mock provider acceptable? | Affects MVP setup complexity and whether API keys/credentials are needed for the customer to test | Confirm at next meeting; customer indicated mock is acceptable ("it's not about the actual LLM") |

## Disagreements

| Your position | Customer's position | What you changed |
| --------------- | --------------------- | ------------------ |
| The architecture diagram showing client → gateway → plugin runner → provider → response plugins → client adequately describes the system | The diagram is too trivial; the plugin runner is a black box with no answer for failure detection, monitoring, timing, or error propagation | Kept position on the high-level flow but accepted that the plugin runner needs a detailed design covering failure handling, monitoring, and error propagation; added action points to study Django/Twisted and document the chosen approach |
| One plugin (PII masking) is sufficient for the prototype | At least three plugins are needed for a minimum usable application; one plugin is trivial | Accepted; MVP will include at least three plugins |
| The prototype demonstrates the core concept | The prototype is "not a real application" — the customer needs something he can download, start, use, and extend | Accepted; committed to delivering an MVP by end of week with setup documentation and a documented way to add plugins |
