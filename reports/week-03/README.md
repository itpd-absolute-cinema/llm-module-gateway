# Week 03 report

## Project

Modular LLM Gateway, Team 1 - Absolute cinema.

Our problem-space sentence: Companies need a way to let employees use LLMs while applying organization-specific policies for data protection, routing, and request processing.

## What we did

We wrote four goals for the threshold of success, planned seven sprints, and listed five risks. We recorded the stack choice, defined done for stories, tasks, and pull requests, and added a contributing guide. We also published the documentation site and brought the Week 1 decisions and assumptions into the maintained docs.

At the customer meeting, we agreed to deliver a usable product with at least three plugins the following week. `REL-03` has not been published, so there is no release tag yet. No individual candidate story has a recorded decision moving it out of scope.

## Findings

The customer confirmed that the main user is a DevOps engineer who needs to apply company policies to LLM requests. Adding a plugin should be possible by following the documentation, without learning the gateway internals.

The plugin runner needs a more detailed design. We need to show which plugin ran, how long it took, and where it failed. The customer suggested Django middleware and Twisted callbacks as references. The failure-handling policy remains open; a mock provider is acceptable for this milestone.

We chose Java 25 and Spring Boot 4 for the Gateway, with Python for the Interpreter, plugins, and CLI. The threshold of success covers plugin development, local deployment, CLI use, and plugin-chain resilience. The goals still need customer agreement.

## Minimum usable product candidate

Core task: a client sends a request to the Java gateway, the request passes through Python plugins for request filtering, routing, and response filtering, and the client receives a processed response from the target LLM.

- [`US-01`: Register and load a Python plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/17)
- [`US-02`: Filter the request before routing](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/18)
- [`US-03`: Route a request to a target LLM via a plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/19)
- [`US-04`: Filter the response before returning it to the client](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/20)

## Customer validation

The customer reviewed the prototype on 16 October. The meeting report records feedback on the flow, rather than acceptance against each story's criteria.

| Story | Meeting outcome |
| --- | --- |
| `US-01` | The customer asked for instructions that let him add a plugin himself. |
| `US-02` | The customer asked for a larger plugin chain. |
| `US-03` | The prototype used a mock provider, which the customer accepted for this milestone. Routing-story acceptance was not recorded. |
| `US-04` | No separate response-filter demonstration or verdict was recorded. |

The customer found one masking plugin insufficient for a usable product. We agreed to add at least two more plugins and document how to run and extend the gateway. Per-story verdicts and their `DEC-nnn` entries are still open.

## Coverage

| Deliverable | Artifact |
| --- | --- |
| Threshold of success | [docs/threshold-of-success.md](../../docs/threshold-of-success.md), four goals |
| Project plan | [docs/project-plan.md](../../docs/project-plan.md), `MLS-03` and `SPRINT-03` to `SPRINT-09` |
| Risk register | [docs/risks.md](../../docs/risks.md), five risks with triggers, mitigations, and contingencies |
| Stack decision | [docs/decisions.md, DEC-011](../../docs/decisions.md#dec-011-tech-stack) |
| Definition of Done | [docs/definition-of-done.md](../../docs/definition-of-done.md) |
| Contributing guide | [CONTRIBUTING.md](../../CONTRIBUTING.md) |
| Sprint milestones and issue fields | [SPRINT-03 milestone and issues](https://github.com/itpd-absolute-cinema/llm-module-gateway/milestone/1) |
| GitHub project | [Project table](https://github.com/orgs/itpd-absolute-cinema/projects/1) |
| Minimum usable product | `US-01` to `US-04` above; prototype feedback in [reports/week-03/meeting-report.md](meeting-report.md#summary) |
| Changelog and first release | Not published yet, date shift approved by the customer |
| Documentation site | [Project documentation](https://itpd-absolute-cinema.github.io/llm-module-gateway/) |
| Setup and run instructions | [docs/setup.md](../../docs/setup.md) |
| Week 2 action points | [reports/week-03/meeting-report.md, Previous action points](meeting-report.md#previous-action-points) |
| Week 2 open questions | [reports/week-03/meeting-report.md, Previous open questions](meeting-report.md#previous-open-questions) |
| Meeting script | [reports/week-03/meeting-script.md](meeting-script.md) |
| Customer validation | [reports/week-03/meeting-report.md](meeting-report.md), [reports/week-03/meeting-transcript.md](meeting-transcript.md) |
| AI usage | [reports/week-03/ai-usage.md](ai-usage.md) |

## Contribution

The linked commits cover the work after the Week 2 merge on 10 October.

| Member | Work |
| --- | --- |
| @b4lmor | Wrote the planning documents, stack decision, Definition of Done, contributing guide, and meeting materials: [planning commit](https://github.com/itpd-absolute-cinema/llm-module-gateway/commit/9ef5f1a06e14ee77787a98a9e3f1d7b483485ec3). Presented the prototype and recorded the [MUP delivery agreement](https://github.com/itpd-absolute-cinema/llm-module-gateway/commit/aef9b4127342134b06737a0f94d2117dfd0d95b0). [PR #33](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/33) |
| @egozhuk | No authored commit in the reviewed Week 3 history. |
| @m1staken | No authored commit in the reviewed Week 3 history. |
| @FallenChromium | Added the documentation site and workflow in [PR #35](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/35), moved Week 1 decisions and assumptions into the maintained docs in [PR #34](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/34), and fixed Markdown formatting and anchors, reviewed the planning documents and requested specific changes to them [PR #33](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/33), participated in the stack discussion. |

## Repository evidence

[Merged PR #35](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/35) closed [task #36](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/36).

Latest successful runs on `main` at the time of writing: [link check](https://github.com/itpd-absolute-cinema/llm-module-gateway/actions/runs/39157085987), [Markdown check](https://github.com/itpd-absolute-cinema/llm-module-gateway/actions/runs/39157086029), and [documentation deployment](https://github.com/itpd-absolute-cinema/llm-module-gateway/actions/runs/39157086200).

No URLs are excluded from the link check.

## Deviations

- We showed a prototype and agreed to deliver the MUP the following week. The complete candidate and `REL-03` were not delivered this week.
- Customer feedback is recorded for the prototype as a whole. Per-story acceptance decisions and a verified clean-checkout run are still open.

## Privacy

Privacy confirmation is pending the team's final review.
