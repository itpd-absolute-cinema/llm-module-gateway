# Week 6 Customer Meeting Report

## Metadata

- **Date:** November 6, 2026.
- **Duration:** Approximately 45 minutes.
- **Present:** Customer and team representatives.
- **Presented:** Plugin installation via a one-line command, a filtered request through the Gateway with a DeepSeek API key, the plugin loading path in the Gateway source, and the team's plan for Docker-based packaging.
- **Permission to publish transcript:** Not stated in the supplied transcript; confirmation needed before publication.
- **Permission to share transcript privately:** Not stated in the supplied transcript.
- **Transcript:** Recording supplied by the customer; sanitized draft prepared locally, publication permission pending.
- **Meeting script:** None supplied.

## Previous action points

Rows from [Week 5 action points](../week-05/meeting-report.md#action-points) due in Week 6.

| Action (quoted) | Owner | Outcome | Artifact changed |
| --- | --- | --- | --- |
| [Week 5: "Add and test plugin timeouts after agreeing the affected request and cleanup behavior."](../week-05/meeting-report.md#action-points) | @FallenChromium | Not done — the customer did not settle the affected-request and cleanup behavior in the Week 6 meeting, so the timeout policy could not be agreed. The action is carried forward to Week 7. | None |
| [Week 5: "Present the installation, demo, and extension workflow for customer acceptance."](../week-05/meeting-report.md#action-points) | @b4lmor | Partially done — the team demonstrated a one-line installation and a request through the Gateway. The customer did not accept the stories; instead, the customer asked for the demo to target DevOps pain points and to be packaged in Docker. The story acceptance remains open. | None |
| [Week 5: "Demonstrate a client or agent sending a request through the gateway and receiving a response."](../week-05/meeting-report.md#action-points) | Pending assignment | Done — the team demonstrated a request through the Gateway to DeepSeek and a response back. The customer acknowledged the connection but said it does not yet address DevOps pain points. | None |

## Previous open questions

Rows from [Week 5 open questions](../week-05/meeting-report.md#open-questions). The Week 6 recording does not settle any of them.

| Question (quoted) | Status | Answer / carried to |
| --- | --- | --- |
| [Week 5: "When a required plugin fails or times out, should the request be rejected, the plugin skipped, or a fallback used?"](../week-05/meeting-report.md#open-questions) | Carried forward — the customer did not address it in the Week 6 meeting. | `## Open questions` below. |
| [Week 5: "Should outer plugins receive exceptions for cleanup, and which diagnostics may reach the client?"](../week-05/meeting-report.md#open-questions) | Carried forward — the customer did not address it in the Week 6 meeting. | `## Open questions` below. |
| [Week 5: "Do the examples meet the earlier minimum, and can the extension workflow be accepted?"](../week-05/meeting-report.md#open-questions) | Carried forward — the customer asked for a DevOps-oriented presentation and Docker packaging, not for the examples themselves. Acceptance is still pending. | `## Open questions` below. |

## Summary

- The team demonstrated a one-line installation of the plugin and a request through the Gateway to DeepSeek.
- The customer said the demonstration does not yet show why a DevOps engineer would choose this product over writing their own functions: the value must be framed around DevOps pain points — managing many plugins, validation, failure handling, deployment.
- The customer asked for Docker or Docker Compose packaging so versions and environments are not managed by hand.
- The customer asked for the next presentation to show the Gateway addressing a DevOps pain point directly, not only a request reaching an LLM.
- No explicit story acceptance verdict was recorded.

## Decisions

None recorded as a formal customer decision.

## Action points

| Action | Owner | Due |
| --- | --- | --- |
| Package the Gateway, Plugin Manager, and CLI in Docker Compose so the system starts with one command and versions are explicit. | @b4lmor | Week 7 |
| Reframe the next demo around a DevOps pain point: show plugin validation, failure isolation, or deployment, not only a request to an LLM. | @egozhuk | Week 7 |
| Write down why the plugin system is better than a set of functions called from a main loop, and use that answer in the architecture and the demo. | @FallenChromium | Week 7 |

## Open questions

| Question | What it would change | Follow-up |
| --- | --- | --- |
| When a required plugin fails or times out, should the request be rejected, the plugin skipped, or a fallback used? | Request recovery policy. | Confirm with Customer. |
| Should outer plugins receive exceptions for cleanup, and which diagnostics may reach the client? | Cleanup and client error behavior. | Confirm with Customer. |
| Do the examples meet the earlier minimum, and can the extension workflow be accepted? | Acceptance of the examples and developer workflow. | Present the walkthrough to Customer. |

## Disagreements

None recorded for this team's presentation.