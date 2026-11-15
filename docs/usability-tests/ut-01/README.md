# UT-01: Add and Verify a Request Filter

**Status:** Planned

**Design sprint:** [SPRINT-07](../../project-plan.md#sprint-07)

**Run sprint:** [SPRINT-08](../../project-plan.md#sprint-08)

**Quality requirement:** [QR-07 (planned)](../../sprint-07-planning.md#define-the-usability-quality-requirement-and-its-manual-test). Link to the requirement once it is added.

## Goal

Find out whether a DevOps engineer can use the Gateway CLI to add a supplied request filter and verify its effect on a synthetic request without help.

## Research Questions

1. Can participants add the supplied filter and verify its effect without help?
2. Can participants tell which filters are active and confirm that disabling a filter changes what the Gateway does?
3. Where do participants hesitate, make mistakes, or need help during these tasks?

## Participant Profile

The test is for adults who use a CLI to configure or troubleshoot services. The recruitment plan will define screening and the target mix of experience.

## The Tasks

Run the tasks in order for every participant. The moderator reads only the scenario column.

| Task | Scenario | Start | Successful end | Limit |
| --- | --- | --- | --- | --- |
| T1 (main) | A test application sends contact numbers through the Gateway. A colleague supplied a filter to mask those numbers. Make the filter take effect and show that the test application no longer returns the sample number. | The supplied filter is absent. The test client and sample request are available. | The filter is active and the participant obtains a test reply containing `[PHONE]` in place of the sample number. | 8 minutes |
| T2 | You are handing over the Gateway to a teammate. Tell them which of the available filters would currently affect a request. | The moderator restores the seed with the phone filter active and one inactive filter. | The participant correctly identifies the active and inactive filters using the Gateway's reported state. | 3 minutes |
| T3 | Your teammate wants to compare the original input with the filtered version. Temporarily turn off phone masking and show that the original sample number comes through again. | The same seed as T2, with the phone filter active. | The phone filter is inactive and the participant obtains the original sample number in the reply. | 5 minutes |

Apply these outcome rules to each task using its successful end and limit above. Mark **Unaided** if the participant reaches the successful end within the limit without a hint. Mark **Assisted** if they reach it within the limit after a hint. Mark **Failed** if they stop or reach the limit without meeting the criterion.

A hint is any direction about the relevant command, setting, next step, or how to interpret the output. Repeating the scenario verbatim or asking the participant to think aloud does not count as help. Give a hint only when the participant explicitly requests help and record what was said and when. Stop the task at its limit or when the participant asks to stop.

## Protocol

Follow the [session guide](sessions/README.md) and [surveys](surveys.md). Read the scenario, start the timer, and observe the participant working aloud. Record the outcome and ask the SEQ before moving to the next task. After T3, administer SUS and the open-ended post-test before the debrief.

## Measures

Record the outcome, help given, elapsed time, and 1–7 SEQ for each task. Record the ten SUS ratings and score for each participant. Capture observations about errors, pauses, and the evidence participants use to judge whether a change worked. Compare the observations with the client events described below.

## Instrumentation

Use the test-only CLI in `code/cli/usability_gateway_cli.py` with the staging Gateway configured to use its stub provider. The normal CLI still handles plugin administration. The test entry point records the outcome of each Gateway call and provides a fixed synthetic chat probe whose expected reply changes when the phone filter is enabled or disabled.

| Record | What it shows | Week 6 equivalent |
| --- | --- | --- |
| `upload`, `enable`, `disable` | The client attempted the change and received a success or error. | `plugin_upload_requested_total`, `plugin_upload_completed_total`, `plugin_enabled_total`, `plugin_disabled_total` |
| `inspect` | The client attempted to read the plugin state. | None |
| `probe` | The client received a masked, unmasked, or unexpected reply, or the request failed. | `request_received_total` and plugin-chain events give server-side context. |

These are separate client-side records. The test does not reuse Week 6 aggregate events for per-session evidence and sends no new events through the shared analytics pipeline. The local records have `source=usability-test`, a random session UUID, action, outcome, UTC timestamp, and request duration. Only a probe records the classified effect. The log excludes request and response bodies, plugin names, credentials, and participant identity.

The moderator creates one private JSONL file on the test workstation per session, records its UUID beside the participant code in the private session sheet, and places the export in the private usability-data repository. The records cover Gateway calls made through the test CLI. They cannot show which filter state the participant understood in T2, where they hesitated, or what help they received. Use the observation sheet for those points.

Before the first counted session, have a teammate run the main task on university staging. Confirm the JSONL file contains a successful upload, enable, and masked probe under the UUID in the private session sheet. Save the verification outcome separately from the Week 7 rehearsal.

## Analysis Plan

The analysis plan will define how to summarize task outcomes, survey responses, observations, and telemetry. The findings and conclusion will go in [results.md](results.md).

## Schedule

Prepare the test in SPRINT-07 and plan counted sessions for SPRINT-08. Verify the staging instrumentation before the first counted session.

## Roles

- Facilitator for every counted session: `@egozhuk`.
- Observer for timing, help and friction: `@FallenChromium`.
- Custodian for raw notes and recordings: `@m1staken`.

The assignments are provisional until the team confirms availability.

## Environment

Use the staging deployment on the university network with synthetic inputs and a resettable starting configuration.

## Consent And Data

Obtain durable consent from each participant before their counted session. The [public consent template](consent.md) covers recording and publication of sanitized results, with separate permission for quotes. If a participant declines recording or withdraws, exclude the session, remove its private materials, and retire the participant code.

Use a participant code in session records. Keep the mapping from the code to the participant's identity with the signed consent in the [private usability-data repository](https://github.com/itpd-absolute-cinema/llm-module-gateway-usability-data). Keep recordings in a team-owned cloud folder. Keep its URL and access instructions in the private repository and the Moodle submission. Give the team and instructors access to the private materials. Delete the private repository, its history, and the recordings after grading. The sanitized public record stays.

## Evidence Map

| Location | Evidence |
| --- | --- |
| Product repository | Test design, blank surveys and consent template, facilitator guide, and sanitized results and findings. |
| Private usability-data repository | Signed consent, identity mapping, raw notes and survey responses, working analysis, filtered telemetry export, and instructions for accessing the recording folder. |
| Team-owned cloud folder | Session recordings, for the team and instructors. |
| Moodle submission | Private access links and instructions for instructors. |
| Week 8 report | Summary and links to the results and quality verdict. |
