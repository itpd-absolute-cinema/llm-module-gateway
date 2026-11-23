# Week 08 Report

**LLM Gateway, Team 1 – Absolute Cinema.** This report covers SPRINT-08 and Assignment 8.

## Summary

SPRINT-08 was a task sprint: 0 planned story points, 0 completed. This week ran the UT-01 usability sessions (3 counted), wrote the results with the per-participant table, the findings, and the triage, and wrote the configuration plan and its goal-based evaluation.

**Usability:** 3 counted sessions. Task 1 (create a plugin) was unaided for 1 of 3, assisted for 2 of 3. Task 2 (enable) was unaided for 3 of 3. Task 3 (create a user) was unaided for 1, assisted for 1, failed for 1. SEQ and SUS were not collected. The QR/QRT verdict is `Fails` on Task 1's completion and assistance bar. The product response: make the "New plugin" and "Users" entry points visible, and add a one-line hint above the code editor.

**Configuration:** 9 Active `CFG-nn` items across five groups, four local goals, and a first change: a one-line pointer from `AGENTS.md` to the configuration plan.

`REL-08` is a docs-only release at `v0.8.0`: no product change is included.

## Usability test

- Results: [`docs/usability-tests/ut-01/results.md`](../../docs/usability-tests/ut-01/results.md)
- Sanitized session notes: [`docs/usability-tests/ut-01/sessions/`](../../docs/usability-tests/ut-01/sessions/)
- QR/QRT: [QR-07](../../docs/quality-requirements/qr/QR-07.md), `Fails`, `Checked: SPRINT-08`
- Findings and triage: [results.md#findings](../../docs/usability-tests/ut-01/results.md#findings)
- Private repository: `itpd-absolute-cinema/llm-module-gateway-usability-data`
- Recordings: Google Drive folder, accessible to the team and the instructors

## What was built

This sprint delivered a full web UI for DevOps engineers to manage the gateway without touching the CLI. The UI is a single application served by the Gateway, with three management areas:

- **Plugins:** create a plugin, edit its behavior, enable or disable it, and see its current status in the request chain.
- **Models:** register and configure the LLM providers the gateway routes to, with their endpoints and credentials.
- **Users:** create and manage the developer accounts that reach the gateway, with their access to the plugin chain.

The UI is built for the operator who runs the gateway day to day: every management action that the CLI exposes is available in the browser, and the request path, the plugin chain, and the usage dashboard are visible from the same place. The production instance runs the UI on the university VM, and the staging instance carries the seeded data the usability sessions used.

## Deviations

None.

## Privacy

No private-only material was committed to the product repository, and no participant identity was committed to it.
