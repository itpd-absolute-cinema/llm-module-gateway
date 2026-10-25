# Week 4 Follow-up Meeting Script

**Status:** Pending Week 5. Customer agreed to postpone the Week 4 meeting; this agenda has not been carried out as that week's Sprint Review. See the [status note](meeting-report.md#sprint-review-status).

## Context

DevOps engineers need to add company rules without changing the Gateway core and to identify failures when those rules run.
This script carries the unanswered Week 3 questions into the postponed review.
The Python experiment still needs customer feedback on its interface and failure policy; the 23 October call with a different team supplies no answers for this project.
The meeting should settle the plugin interface and failure behavior, review the proposed architecture, and agree the remaining delivery work.

## Agenda

1. Permissions, 3 minutes: nothing shown; questions 1–3.
2. Working software, 8 minutes: show the [pinned prototype instructions](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/ca30bcf8979aefb6fe0d9d870429e022214a40ed/prototype/README.md) and the running demo, using each presented story's `AC-nn`; questions 4–5.
3. Quality and architecture, 6 minutes: show [PR #48](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/48) against the [vision](../../docs/product-vision.md) and [thresholds](../../docs/threshold-of-success.md); question 6.
4. Previous open questions, 10 minutes: show the [plugin interface, error path, and monitoring work](../../docs/architecture/plugin-chain.md); questions 7–9.
5. Read-back, 3 minutes: show the [follow-up record](meeting-report.md), confirm verdicts, owners, and due weeks; question 10.

## Questions

1. May we record this meeting? — closed
2. May we publish a sanitized transcript in the repository? — closed
3. If publication is refused, may we share the transcript privately with instructors? — closed
4. Do you accept each demonstrated story? If not, which `AC-nn` failed? — closed
5. Do the examples meet the earlier minimum? Can a developer follow the instructions to add and manage a plugin, or is something missing? — open
6. Do the quality requirements and container split in PR #48 meet the goals and constraints in the vision? What needs to change? — open
7. When a required plugin fails or times out, should we reject the request, skip the plugin, or use a fallback? — open, critical
8. Should exceptions unwind through outer plugins for cleanup, and should plugin identity appear only in administrator logs or also in the client error? — open, critical
9. Can we use the documented async factory and handler instead of separate hooks? Which ID and version fields do plugins need? — open, critical
10. Have we recorded the decisions correctly? Who will take each remaining action, and when is it due? — open

## Roles

- Moderator: @b4lmor.
- Note taker: @FallenChromium.
- Observer: @egozhuk.

## Key improvements

- Before: "Which failure-handling policy do you prefer?" After: question 7 asks what happens to the request when a required plugin fails. The Week 3 report leaves the request recovery policy open.
- Before: "Is our plugin interface acceptable?" After: question 9 names the proposed interface and asks which fields it needs. The answer can lead to a concrete change.
