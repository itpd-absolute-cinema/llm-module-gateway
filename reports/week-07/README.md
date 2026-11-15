# Week 07 Report

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
