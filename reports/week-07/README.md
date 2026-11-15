# Week 07 Report

<<<<<<< HEAD
Working draft integrating assignment 7 parts 1–7; the remaining weekly delivery and external checks are not yet recorded as complete.

The [project plan](../../docs/project-plan.md#sprint-07) and [planning draft](../../docs/sprint-07-planning.md) cover the sprint and scheduled task issues.

## Usability test

[UT-01](../../docs/usability-tests/ut-01/README.md) checks whether a developer new to this gateway can set it up and obtain a first response, starting with Docker installed and the gateway stopped.
The recruitment target is five completed sessions with adults who have used Docker; three is the minimum.
The secondary tasks enable a supplied phone-masking filter and then disable it, verifying the response in both states.
Task limits are 20, 10, and 5 minutes respectively; the proposed session allowance is about 50 minutes.
Documentation, web search, and AI assistance are allowed; tool use is recorded separately from human help, and the results will describe this combined workflow.
See the record for the screening criteria, proposed recruitment and session plan, analysis, and evidence locations.

[QR-07 QRT-01](../../docs/quality-requirements/qr/QR-07.md#qrt-01) reads the main-task median ease rating against a bar of at least 5/7.
It is [Not verified](../../docs/project-plan.md#quality-requirement-testing-plan), with the verdict planned for SPRINT-08.
The [results file](../../docs/usability-tests/ut-01/results.md) is an empty template; no sessions, findings, or usability results are claimed.

The team plans to supply staging Keycloak and a test account; participants will not provision authentication infrastructure.
The [surveys](../../docs/usability-tests/ut-01/surveys.md), [consent template](../../docs/usability-tests/ut-01/consent.md), and [session guide](../../docs/usability-tests/ut-01/sessions/README.md) include the agreed tasks and allowed tools.
The test-only CLI records gateway calls privately; it does not observe Docker setup, login, or AI use.
Before sessions, confirm the provisional roles, fill consent tool/contact details, verify staging instrumentation and resets, and confirm access and the booking plan.

## Privacy

No participant data has been collected for this draft.
The [usability-data repository](https://github.com/itpd-absolute-cinema/llm-module-gateway-usability-data) exists and was verified private on 15 November 2026. Its authenticated URL is excluded from the link check.
Instructor access, the recordings folder, and the data layout remain unverified.
Raw text and recordings stay outside this public repository, as the test record's evidence map specifies.

## AI usage

See [AI usage](ai-usage.md) for the decisions made by the user and the drafts prepared by Codex.
=======
**LLM Gateway, Team 1 – Absolute Cinema.** This report covers SPRINT-07 and Assignment 7.

## Summary

SPRINT-07 was a task sprint: the team planned 0 story points and completed 0, because the sprint carried the usability test design and the test instrumentation, not product stories. Nothing moved out of the sprint. This week delivered the UT-01 usability test design: the record with its research questions, participant profile, recruitment target, tasks, protocol, measures, instrumentation, analysis plan, schedule, roles, environment, consent and data map, and evidence map; the surveys with the full SUS and SEQ items; the public consent template; the private usability-data repository; the new usability quality requirement with its manual test; and the usability-test risks and glossary terms.

The customer meeting took place on 2026-11-13. The team demonstrated a real OpenCode/OpenAI-compatible harness connection, the statistics dashboard, the middleware chain view, and a recorded demo of the masking and blocking plugins. The customer said the product now looks like a product rather than a prototype, and made one request: record a demo of adding a plugin to the system. The UT-01 design review did not happen in this meeting and is carried to Week 8. No story was formally accepted and no `DEC-nnn` was recorded.

REL-07 is a documentation-only release at `v0.7.0`: no product change is included and no upgrade beyond deploying this version is required.

## Customer outcome

| Story shown | Verdict | DEC |
| --- | --- | --- |
| Real harness connection, statistics dashboard, middleware chain, masking and blocking plugins | Accepted as the right direction — the customer said it looks like a product, and asked for a recorded demo of adding a plugin | None |
| UT-01 design review | Not reviewed — the meeting focused on the working demo; carried to Week 8 | None |

The customer's single new request is due in Week 8: record a demo of adding a plugin through the CLI. The Week 6 action points for Docker Compose packaging and the written architecture argument were not completed and are carried to Week 8. All three Week 6 open questions — plugin failure policy, cleanup and diagnostics, and acceptance of the extension workflow — were carried forward; the customer did not answer them in this meeting.

## Repository evidence

| Deliverable | Artifact |
| --- | --- |
| Usability test group | [`docs/usability-tests/README.md`](../../docs/usability-tests/README.md), with one row for UT-01 |
| UT-01 record | [`docs/usability-tests/ut-01/README.md`](../../docs/usability-tests/ut-01/README.md), with its record sections and stub `results.md` |
| Surveys | [`docs/usability-tests/ut-01/surveys.md`](../../docs/usability-tests/ut-01/surveys.md), with the pre-test, SEQ, full SUS, and post-test |
| Consent template | [`docs/usability-tests/ut-01/consent.md`](../../docs/usability-tests/ut-01/consent.md) |
| Quality requirement | The new usability QR in [`docs/quality-requirements/qr/`](../../docs/quality-requirements/qr/), with its QRT-01 and its matrix row |
| Testing plan | [`docs/project-plan.md`](../../docs/project-plan.md#quality-requirement-testing-plan), with the usability QRT row `Not verified`, `Checked: None`, `Planned for sprint: SPRINT-08` |
| Risks | [`docs/risks.md`](../../docs/risks.md), with the recruitment, consent, data-access, and schedule risks |
| Glossary | [`docs/glossary.md`](../../docs/glossary.md), with the usability-test terms |
| Deployment | [`docs/deployment.md`](../../docs/deployment.md), unchanged from Week 6 except where the record links it |
| Sprint plan | [`docs/project-plan.md#sprint-07`](../../docs/project-plan.md#sprint-07), with its goal, due date, planned velocity, and actual velocity |
| Release | `v0.7.0`, the changelog's `[0.7.0]` section, and the plan's `REL-07` block |
| Sprint retrospective | [`reports/week-07/sprint-retrospective.md`](sprint-retrospective.md) |
| Meeting report | [`reports/week-07/meeting-report.md`](meeting-report.md) |
| Meeting script | [`reports/week-07/meeting-script.md`](meeting-script.md) |

The private usability-data repository is linked from the UT-01 record and excluded from the link check because it needs authentication.
>>>>>>> ac78963 (a7t8-14)
