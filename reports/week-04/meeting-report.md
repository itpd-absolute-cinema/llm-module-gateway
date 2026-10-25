# Week 4 Customer Meeting Report

## Metadata

- **Date:** 2026-10-23; repository reviewed on 2026-10-25.
- **Duration:** About 27 minutes in the supplied transcript.
- **Present:** Customer and other participants; their GitHub usernames are not given in the transcript.
- **Presented:** Plugin installation and registration, request/response processing, setup instructions, and handling of badly behaved plugins were discussed. The transcript contains no acceptance verdicts for this repository's stories.
- **Permission to record:** The supplied excerpt does not include the permission question or its answer.
- **Permission to publish transcript:** Not stated in the supplied excerpt.
- **Permission to share transcript privately:** Not stated in the supplied excerpt.
- **Transcript:** Reviewed source: `GMT20261023-050545_Recording.transcript.vtt`; the original file is not republished here. Times below refer to that file.
- **Meeting script:** No script for 23 October was supplied; [follow-up questions](meeting-script.md) were prepared afterwards for the remaining points.

## Previous action points

The actions below come from `reports/week-03/meeting-report.md`, dated 16 October.
The report says Week 08/09, although it is stored under Week 3.
All five actions are reviewed below; the original report is unchanged.
The implementation reviewed here is the Python experiment in PR #39.
A release of the Java/Python product has not been verified.

| Action | Outcome | Decision |
| --- | --- | --- |
| [Week 3: "Deliver MUP by end of the next week: runnable app with at least 3 plugins, documented plugin addition, visible request/response transformations"](../week-03/meeting-report.md#action-points) | Partly carried out. [PR #39](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/39) has five plugins, a demo provider, and run/add-plugin instructions; local in-process checks passed. Delivery and story acceptance are still unconfirmed. The customer emphasized adding and managing plugins over building more examples (17:35–19:38); see the [setup follow-up](../../docs/setup.md#week-4-follow-up). | [DEC-012](../../docs/decisions.md#dec-012), [DEC-014](../../docs/decisions.md#dec-014) |
| [Week 3: "Study Django middleware system and Twisted callback/error interface; document chosen plugin-chain architecture with rationale"](../week-03/meeting-report.md#action-points) | Carried out for the experiment. The [comparison](../../docs/architecture/plugin-chain.md#why-middleware) explains keeping async middleware and using explicit exception handling rather than adding Twisted. This does not replace the proposed Java/Python product architecture. | None |
| [Week 3: "Document plugin interface requirements (constructor, ID, API version, settings model, on_request/on_response hooks) so a developer can add a plugin quickly"](../week-03/meeting-report.md#action-points) | Partly carried out. The [interface note](../../docs/architecture/plugin-chain.md#interface-in-the-prototype) documents the factory, options, context, response, and extension steps. The customer asked for documentation a DevOps engineer or coding assistant could follow (18:12–18:41), but did not approve a specific interface. Versioning and compatibility remain open. | [DEC-012](../../docs/decisions.md#dec-012), [DEC-014](../../docs/decisions.md#dec-014) |
| [Week 3: "Add per-plugin timing/logging so the customer can see which plugin ran, in what order, and where it failed"](../week-03/meeting-report.md#action-points) | Not carried out. The current logger measures the whole request and does not identify each plugin. The customer confirmed the need to detect and log a stuck plugin without stopping the system (19:41–20:42). The [monitoring note](../../docs/architecture/plugin-chain.md#monitoring-still-needed) records the missing implementation and check. | [DEC-013](../../docs/decisions.md#dec-013) |
| [Week 3: "Define and implement failure-handling policy (fail-fast vs graceful degradation vs configurable per-plugin)"](../week-03/meeting-report.md#action-points) | Partly carried out. The customer was clear about one requirement: a badly written or stalled plugin must not stop the system (19:41–20:42). The prototype still has no per-plugin deadline, and the transcript does not settle what happens to a request whose required rule fails. The [failure note](../../docs/architecture/plugin-chain.md#failure-handling) covers both exceptions and timeouts. | [DEC-013](../../docs/decisions.md#dec-013) |

## Previous open questions

| Question | Answer | Decision |
| --- | --- | --- |
| [Week 3: "Which failure-handling policy does the customer prefer: fail-fast, graceful degradation, or configurable per-plugin?"](../week-03/meeting-report.md#open-questions) | Partly answered at 19:41–20:42: detect the faulty or stalled plugin, stop its execution, log the problem, and keep the system running. Whether the affected request is rejected, skips the plugin, or uses a fallback still needs clarification; see [failure handling](../../docs/architecture/plugin-chain.md#failure-handling). | [DEC-013](../../docs/decisions.md#dec-013) |
| [Week 3: "Should errors propagate back through previously executed plugins (for cleanup/transformation) or return directly to the client with a plugin identifier?"](../week-03/meeting-report.md#open-questions) | Still open. The transcript does not settle cleanup or client diagnostics. The [prototype contract](../../docs/architecture/plugin-chain.md#interface-in-the-prototype) currently allows exceptions to unwind through outer handlers. | None |
| [Week 3: "Is the current plugin interface (separate on_request/on_response hooks) acceptable, or does the customer need a combined plugin with shared state (Django middleware-style)?"](../week-03/meeting-report.md#open-questions) | Still open. Request and response hooks were discussed at 13:35–14:20, but that discussion does not approve this repository's factory/handler interface or choose between the two designs. The current interface is described in the [interface note](../../docs/architecture/plugin-chain.md#interface-in-the-prototype). | None |
| [Week 3: "What is the minimum set of plugins and behaviors (e.g., PII masking, logging, content filtering) required for the MVP to be considered usable?"](../week-03/meeting-report.md#open-questions) | The customer clarified at 17:35–19:38 that basic example plugins are useful, but the main value is the management system and the ability to add custom plugins. No new count or required list was agreed, so the earlier minimum of three plugins still applies. See the [setup follow-up](../../docs/setup.md#week-4-follow-up). | [DEC-012](../../docs/decisions.md#dec-012) |
| [Week 3: "Should the MVP connect to a real LLM provider or is a mock provider acceptable?"](../week-03/meeting-report.md#open-questions) | Already answered in the earlier report's [Previous open questions](../week-03/meeting-report.md#previous-open-questions): a mock is acceptable for that milestone; a real provider is optional. This was not changed in the discussion. The [setup follow-up](../../docs/setup.md#week-4-follow-up) uses a local demo provider. | None |

## Summary

- The customer wants a plugin management system that a DevOps engineer can extend and another engineer can maintain, rather than a collection of manually connected functions.
- Installation and plugin development need clear instructions, ideally enough for a coding assistant to produce a compatible plugin.
- A stalled or faulty plugin must be detected and logged without making the whole service stop.
- Story acceptance, quality requirements, and the proposed container split were not approved in this discussion.

## Decisions

The decisions below record the customer's guidance.
They do not mark any story as accepted.

- [DEC-012: Focus on a plugin management system that DevOps engineers can extend and maintain, with basic plugins as examples.](../../docs/decisions.md#dec-012)
- [DEC-013: Detect and log stalled or faulty plugins while keeping the Gateway responsive.](../../docs/decisions.md#dec-013)
- [DEC-014: Provide reproducible installation instructions and a clear guide for adding a plugin.](../../docs/decisions.md#dec-014)

## Action points

The unfinished Week 3 actions continue below.
Owners and deadlines were assigned during the follow-up on 25 October, after the customer discussion.

| Action | Owner | Due |
| --- | --- | --- |
| Check installation from a clean checkout, run the HTTP demo, and record delivery and story acceptance. | @egozhuk | Week 5, 2026-10-31 |
| Finish the plugin interface documentation and check that a developer can follow it to add a plugin without changing the core. | @b4lmor | Week 5, 2026-10-31 |
| Log each plugin's duration, processing stage, and failures; test with a plugin that deliberately fails. | @FallenChromium | Week 5, 2026-10-31 |
| Add and test plugin timeouts. Agree what happens to the affected request and how cleanup runs before adding recovery. | @FallenChromium | Week 5, 2026-10-31 |

## Open questions

| Question | What it would change | Follow-up |
| --- | --- | --- |
| When a required plugin fails or times out, should we reject the request, skip the plugin, or use a fallback? | Decides whether processing stops or continues after a rule fails. | Ask question 7 in the [follow-up script](meeting-script.md). |
| Should outer plugins receive exceptions for cleanup, and which diagnostics may reach the client? | Determines the exception path and public error response. | Ask question 8 with the current error path on screen. |
| Should the final plugin interface use separate hooks or one async handler, and what identification and version fields are required? | Determines the extension contract and compatibility rules. | Ask question 9 with a minimal plugin example. |
| Do the current example behaviors satisfy the earlier minimum, and can the extension workflow be accepted against its criteria? | Determines acceptance of the plugin examples and developer workflow. | Ask question 5; keep the earlier minimum until a change is agreed. |

## Disagreements

| Your position | Customer's position | What you changed |
| --- | --- | --- |
| We tracked progress partly by the number of example plugins. | At 17:35–19:38, the customer emphasized plugin management, extension, and maintainability rather than the examples themselves. | Updated the setup and interface notes to focus on installing the system and adding a plugin. |
| The prototype already returns an error when a plugin raises an exception. | At 19:41–20:42, the customer also required handling plugins that stall without stopping the service. | Updated the failure note to show that exceptions are handled, but plugin timeouts still need implementation. |
