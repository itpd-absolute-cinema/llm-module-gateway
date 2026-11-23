# UT-01: Plugin and user management through the web UI

**Status:** Completed
**Design sprint:** [SPRINT-07](../../project-plan.md#sprint-07)
**Run sprint:** [SPRINT-08](../../project-plan.md#sprint-08)

**Goal:** Test whether a DevOps engineer can create a plugin, enable it, and create a user through the web UI without help.

## Research questions

1. Can a DevOps engineer create a new plugin through the web UI unaided?
2. Can they enable the plugin and confirm it is active unaided?
3. Can they create a new user through the web UI unaided?

## The Requirement

This test serves [QR-07](../../quality-requirements/qr/QR-07.md), Interaction Capability / Self-descriptiveness, with the median Task 1 SEQ as its measure and a bar of at least 5 of 7.

## Participant Profile

**Role and task:** A DevOps engineer who manages a gateway or proxy for a development team and creates plugins or middleware.

**Familiarity with similar products:** Medium — has used OpenAI-compatible clients or a reverse proxy, but not this gateway UI.

**Device:** Laptop, 15-inch screen, Linux or macOS.

**Relation to target user:** Works in a team that runs an LLM gateway and manages its plugins.

**Recruitment target:** At least 3 representative participants aged 18 or older, 5 as the design target. Not a teammate, the customer, the instructor, or the course staff.

## The Tasks

### Task 1 — Create a new plugin

**Scenario:** Your team needs a filter that masks phone numbers in requests before they reach the LLM. Create that plugin in the system.

**Start point:** The web UI is open on staging, logged in as an operator. The main navigation is visible.

**End state:** A new plugin exists in the system, with the behavior that masks phone numbers.

**Success criterion:** The plugin appears in the plugin list with the name the participant chose.

**Time limit:** 10 minutes.

**Help rule:** The facilitator may say "look at the navigation" once, after 5 minutes of no progress.

### Task 2 — Enable the plugin

**Scenario:** The plugin exists but is not active. Make it start processing requests.

**Start point:** The plugin is created and disabled.

**End state:** The plugin is enabled and will run on the next request.

**Success criterion:** The plugin status is `enabled` in the web UI.

**Time limit:** 5 minutes.

**Help rule:** The facilitator may say "look at the plugin row" once, after 3 minutes of no progress.

### Task 3 — Create a new user

**Scenario:** A new developer joins the team and needs access to the gateway. Create a user account for them.

**Start point:** The plugin is enabled. The web UI is on the users page or the main navigation.

**End state:** A new user exists in the system with a name and a credential the developer can use.

**Success criterion:** The new user appears in the users list.

**Time limit:** 7 minutes.

**Help rule:** The facilitator may say "look at the navigation" once, after 4 minutes of no progress.

## Protocol

See [`sessions/README.md`](sessions/README.md).

## Measures

- Task completion and assistance: `Unaided`, `Assisted`, or `Failed`.
- Time on task: wall-clock seconds per task.
- SEQ after each task: 1–7.
- SUS after all tasks: 10 items, formula in [`surveys.md`](surveys.md).
- Instrumentation: `source=usability-test` events on staging, plus the web UI's own action log.

## Instrumentation

**What the test must observe:** each task's outcome, assistance, time, SEQ, and SUS; and the plugin and user lifecycle events in the web UI.

**Week 6 events reused:** `plugin_upload_requested_total`, `plugin_upload_completed_total`, `plugin_enabled_total`, `request_received_total`, `plugin_chain_completed_total`.

**Events added:**
- `usability_task_started_total` with `participant_code`, `task`, `source=usability-test`.
- `usability_task_completed_total` with `participant_code`, `task`, `outcome`, `source=usability-test`.
- `usability_plugin_created_total` with `participant_code`, `source=usability-test`.
- `usability_user_created_total` with `participant_code`, `source=usability-test`.

All four are rows in [`docs/analytics.md#events`](../../analytics.md#events).

**Environment:** staging deployment on the university network, seed reset to synthetic data before each session.

**Store:** the staging observability backend; the telemetry export goes to the private usability-data repository.

**Session link:** the `participant_code` (`P-01`…) appears in staging test telemetry. Its identity mapping stays private and is the exception to the opaque-identifier rule.

**What the instrumentation cannot cover:** why a participant hesitated on the plugin creation form or the user creation form. The observation notes cover it.

## Analysis Plan

**Per-participant table columns:** participant, Task 1/2/3 outcome, assistance, time, SEQ, SUS.

**Aggregates:**
- Task 1 completion rate and assistance count (the main task).
- Median time per task.
- Median SEQ per task.
- SUS mean and median with `n=<count>`.

**Measure and bar:** median Task 1 SEQ ≥ 5.

**Thematic analysis:** themes from the session notes.

**Severity rating:** Critical, Major, Minor for each finding.

**Instrumentation comparison:** the telemetry export vs the observation; which task or question the export could not cover.

**Limitations:** n < 5, technically familiar participants, synthetic seed, single web UI session.

## Schedule

Sessions run in SPRINT-08, in person, on the university network, against staging.

## Roles

- **Facilitator:** @b4lmor — runs every counted session.
- **Observer:** @FallenChromium — records help and friction.
- **Recordings and raw notes:** @egozhuk — keeps the recordings and the raw notes.

## Environment

Staging deployment on the university network, seed reset to synthetic data before each session, no real payment, instrumentation verified before the first counted session.

## Consent And Data

Every counted session is preceded by the durable consent in [`consent.md`](consent.md). The consent covers the screen recording only; no audio is recorded. The raw screen recording and the telemetry export go to the private usability-data repository; the participant code mapping and the consent records stay there too. No participant identity is in the product repository.

**Retention:** the private repository, its history, and the recordings are deleted after the course is graded. The sanitized public record stays.

## Evidence Map

| Evidence | Where | Public? |
|---|---|---|
| Sanitized session notes | `sessions/P-nn.md` | Yes |
| Results and findings | `results.md` | Yes |
| Consent template | `consent.md` | Yes |
| Surveys | `surveys.md` | Yes |
| Consent records and identity mapping | private usability-data repository | No |
| Raw screen recordings | team-owned cloud folder | No |
| Telemetry export | private usability-data repository | No |