# UT-01: Set up the gateway and get a first response

- **Status:** Planned
- **Design sprint:** [SPRINT-07](../../project-plan.md#sprint-07)
- **Run sprint:** [SPRINT-08](../../project-plan.md#sprint-08)

Find out whether a new user can set up the gateway and get a first response with documentation, web search, and AI assistance available.

This design is being prepared before the sessions.
It is not ready to run: facilitator, consent, and instrumentation still need to be settled, and the supplied staging access must be verified.

## Research Questions

1. Can a new user reach a first response without facilitator help, using the allowed tools, and where do they get stuck?
2. How easy does the user find setup after attempting it?
3. Can the user enable and disable a supplied filter and verify its effect on a response?

The test serves [QR-07](../../quality-requirements/qr/QR-07.md).
Its pass criterion is the main task's median ease rating of at least 5 out of 7.
That rating does not establish that everybody completed setup unaided.

## Participant Profile

Recruit developers who have used Docker but have never set up or used this gateway.
They represent application developers evaluating a self-hosted gateway; professional DevOps experience is not required.

Use this screening checklist before scheduling:

- The person has developed or maintained an application in work, study, or a personal project, and uses a terminal to run development commands.
- They have personally started and stopped a Docker container before; Compose and LLM-gateway experience are recorded but not required.
- They have not previously used this gateway or rehearsed its setup instructions.
- They are 18 or older, outside the project team, and not the customer, instructor, or course staff.

Ask for a brief, non-identifying description of Docker experience; record familiarity as occasional or regular, and whether they have used Compose or another LLM gateway.
Also record whether they regularly use AI tools for development; this describes their background and is not an eligibility requirement.
Use the team's laptop for every session, accessing the prepared staging machine.
In the sanitized session note, record how the person matches the profile, their familiarity, and the device, without their name or employer.

Aim for five completed sessions; at least three are required.
Classmates qualify when they match this profile.
Begin recruitment with developers in other course teams; if there are too few eligible volunteers, ask campus developer groups or other developer contacts who can attend in person.
The team sends invitations; none have been sent as part of preparing this design.

## The Tasks

Run T1, T2, and T3 in that order for every participant.

### T1: Set up the gateway and get a first response

The main task is setting up the gateway and receiving a response to a synthetic text request from the stub provider.

**Participant scenario:** You are evaluating this gateway for an application your team maintains. You have its source code and documentation on a prepared machine, but the gateway is not running. Make it available and obtain a response to a sample text request.

**Start:** A fresh checkout of the chosen release on a staging machine, with Docker and Compose installed and working, the gateway stopped, and no configuration or saved state from an earlier participant.
The participant has access to the machine and the product documentation before timing begins.

**End state:** The gateway runs from the participant's setup and returns a successful chat response containing the stub's echo of the synthetic input.
The observer records the request and response privately as evidence; merely starting containers is not completion.

**Time limit:** 20 minutes. Check its practicality in the teammate dry run before counted sessions.

### T2: Enable a supplied filter and verify its effect

**Participant scenario:** Your team wants phone numbers hidden in requests sent to a provider. A phone-masking filter has been supplied but is inactive. Make it protect requests and demonstrate the difference using the sample text.

**Start:** A running staging gateway with only the supplied `mask_phone` request filter registered, disabled, and using its default settings.
The sample text is `Call +1 202 555 0100`, a fictional number.
The team supplies its staging Keycloak instance and an authorized test account, with access details shared privately.
The participant starts signed out of the CLI and signs in using that account; normal sign-in is included in T2's time and observations.
Provisioning Keycloak, creating accounts, and assigning roles are team preparation, outside the participant's tasks.

**End state:** `mask_phone` is enabled, and the stub returns `[stub] Call [PHONE]` for the sample text.
Changing the input manually to hide the number does not meet the criterion; the original synthetic number must be sent through the gateway.

**Time limit:** 10 minutes, including normal CLI sign-in.

### T3: Disable the filter and verify the difference

**Participant scenario:** For a controlled comparison with synthetic data, your team needs the original text to reach the provider again. Temporarily stop the phone-masking behavior while keeping the filter available for later use, and demonstrate the difference.

**Start:** The gateway is running with `mask_phone` enabled, and the participant has the same sample text and access as in T2.

**End state:** `mask_phone` remains registered but disabled, and the same request returns `[stub] Call +1 202 555 0100`.
Deleting the filter does not meet the criterion.

**Time limit:** 5 minutes.

### Outcomes and help

Record `Unaided` for reaching the end state without facilitator or other human help, `Assisted` for reaching it with recorded human help, and `Failed` for not reaching it within the limit.
Documentation, web search, and AI assistance are allowed in every task; their use is optional and does not itself change `Unaided` to `Assisted`.
Label reported unaided outcomes as “without human help, tools allowed”; never describe AI-assisted success as success using documentation alone.
Use the same team-provided AI tool and configuration for everyone, with an empty conversation and workspace at the start of each session.
Keep that conversation across T1–T3 within the session, so normal continuity is preserved; record the tool/model and configuration used.
Participants may ask the tool for explanations or commands, but a request to a person for a command or solution counts as human help.
Record tool use separately: documentation, web search, AI, and whether AI only advised or also executed actions.
The participant must still demonstrate each task's end state; an AI claim that it succeeded is not evidence.
The facilitator may clarify the scenario but cannot explain commands or solve the task.
Record any help that is nevertheless given; do not relabel it as clarification.

Collect each task's SEQ before preparing the next task.
Between tasks, outside the participant's task time, the facilitator establishes the next task's specified starting state without explaining the solution.
Record any reset, including recovery after an earlier failed task; it does not change that task's outcome.
This keeps a failed setup from preventing observation of both secondary tasks.
Finish and verify the reset procedure before sessions.

Start the timer after reading the scenario and confirming that the specified starting state is ready, when the participant begins work.
Stop when the full end state is demonstrated, the participant gives up on that task, or the task limit is reached.
Reading, searching, AI interactions and waits, sign-in, retries, downloads, and startup waits during the attempt remain in task time.
Do not pause to provide help or extend the limit for an individual participant.
At the limit, record `Failed`, ask the SEQ, and prepare the next task.
Giving up on one task can still produce a counted session if the participant chooses to continue and completes the required measures; withdrawing from the session is different.

Separate future tests can cover writing plugins and more extensive administration or diagnosis.

## Protocol

Pending the session instructions and surveys from assignment parts 2–4.
Use the fixed order: welcome, consent and recording check, pre-test, think-aloud instruction, the three tasks with an ease rating after each, the usability questionnaire, open questions, and debrief.
The same facilitator runs every counted session with an observer present.

## Measures

Record each task's outcome, help given, elapsed time, and Single Ease Question (SEQ) rating.
For each task also record web search and AI use (yes/no), the AI interaction mode (advice or execution), and a brief observation of what the tools helped with or got wrong.
Ask after every attempt, including a failed task: “Overall, how difficult or easy was the task to complete?”
The scale is 1 (very difficult) to 7 (very easy).
Record all ten System Usability Scale (SUS) responses after the three tasks.

## Instrumentation

Pending: identify and verify the actual records available for setup and the request, their private storage, and the session link.
Do not assume the monitoring described in the week 6 documents is implemented.
Human observation records reading difficulties, help, and perceived ease; server events cannot establish those.
Events sent through the analytics pipeline must carry `source=usability-test`.

## Analysis Plan

Fix this plan and all task criteria before the first counted session.
Use one row per participant and task: participant code, task, outcome, human help given, time in seconds, SEQ, web search used, AI used, and AI interaction mode.
Keep a separate participant table for the SUS score, linked to the sanitized session note.

- Report main-task completion as counts and proportions, both including help and unaided only.
- For each task, report how many participants used web search and how many used AI, and show their outcomes beside that use. Treat these as descriptions, not a controlled comparison of AI's effect; participants choose whether to use it.
- Report the main-task median observed time across all counted attempts, including failures at their stopping time; label it as attempt time, not successful completion time.
- Report median SEQ for each task. The main-task median includes failed attempts and is the only statistic used by QR-07; the bar is at least 5/7.
- Calculate SUS as `2.5 × (20 + sum(odd responses) - sum(even responses))`, without first reversing the even responses. Report its mean, median, and participant count.
- For an even number of observations, the median is the mean of the two middle values. Do not round a statistic before comparing it with the pass criterion.
- Group recurring observations into themes, citing the sessions. A single observation may be a finding, but is not presented as a recurring theme.
- Rate findings `Critical` if they prevent the journey or expose a serious safety problem, `Major` if they require help or substantial rework, and `Minor` for recoverable friction. Explain each rating.
- Compare observation with the verified instrumentation, naming gaps and disagreements. Define the exact comparison when the instrumentation is selected, before sessions.
- State the sample size, participants' relation to intended users, prepared environment, observer presence, and unmeasured behavior. The result concerns a workflow with search and AI available, not documentation alone; it cannot establish whether AI caused better outcomes. Docker installation, obtaining machine access, Keycloak and account provisioning, and live-provider configuration are outside this test. These sessions do not establish a population success rate.

Do not invent missing responses.
A failed task counts when the session's required data is complete; a pilot, withdrawn session, or technically broken session does not.
Product errors and confusing documentation are task failures, not reasons to discard an inconvenient result.
An unusable recording or an unrelated campus outage is a session failure; record the cause and keep it separate from product findings.
Record exclusions and use the next unused participant code for replacements.

Every finding is triaged with a severity, an action or reason for no action, and a planned sprint.
If significant findings exist, turn at least one into a SPRINT-08 task issue.
Behavior changes and defects link the affected story, its acceptance criterion, and the applicable functional requirement or architectural decision.
The issue cites the finding's stable anchor in `results.md`, and the finding links back to the issue.

## Schedule

Sessions and results are planned for SPRINT-08, with results due on 21 November 2026.
Allow about 50 minutes per participant: up to 35 minutes for the three tasks and approximately 15 for consent, surveys, transitions, and debrief.
Keep extra time between bookings for resets and verify this allocation in the teammate pilot.
The following booking plan is proposed, not a claim that participants or team members are available:

| When | Planned work |
| --- | --- |
| 16–17 November | Screen volunteers against the profile and arrange five sessions; keep willing replacements available. |
| Before the first counted session | Finish the design, check consent and private storage, run the teammate pilot, and verify instrumentation and the participant link. |
| 18–19 November | Run the booked sessions and prepare sanitized notes after each one. |
| 20 November | Reserve time for cancellations or replacement sessions. |
| 21 November | Complete analysis and triage, record the quality verdict, and link the evidence in the Week 8 report. |

The team must assign the roles below and confirm availability before booking.
If preparation is not ready, reschedule sessions and record the delay rather than counting an unverified run.
Sessions run in person on the university network.
Assign `P-01`, `P-02`, and subsequent codes in scheduling order; never reuse a code.

## Roles

Pending: name the facilitator, observer, and raw-data custodian by GitHub username.

## Environment

Use staging on the university network, synthetic data reset before each session, and the stub provider with no paid LLM call or real payment.
Use the team's laptop to access the prepared staging machine; establish that access before the task begins.
Docker and Compose must already work, but the gateway starts stopped and unconfigured in a fresh checkout of the same commit for every participant.
Reset only the dedicated test deployment's configuration and volumes before each session, leaving no earlier participant's data or settings.
Give each participant the same terminal, documentation access, and synthetic request data.
Verify the shared web-search and AI tools before sessions and record their versions or visible model identifiers, available context, and permissions.
Clear earlier participants' AI history and workspace state between sessions, while preserving any required raw evidence privately.
After T1, seed only the repository's supplied `mask_phone` filter, disabled with default options; before T3 ensure it is enabled.
The reference source is `code/plugin-manager/examples/mask_phone.py`.
Verify the masked and unmasked responses during the dry run; these are expected outcomes from source inspection, not a claim of a completed staging check.
The CLI requires Python and authorized Keycloak access; prepare Python and use the team's supplied staging Keycloak instance and test account.
Before the pilot, verify the gateway and CLI use that issuer, browser sign-in works from the session device, and the account has the `gateway-admin` role.
These checks have not yet been performed for UT-01.
Use the same permissions for everyone and clear saved CLI tokens and browser login state before T2 of each session.
Keep test credentials and the staging address private; do not use participants' personal accounts.
Record the tested commit, machine and tool versions, image versions, and image-cache state in the run preparation; keep these consistent across sessions.
Verify image availability and campus connectivity before inviting participants; retain any download and startup waits that occur during their attempts in the measured task time.
Do not change production.

The setup guide currently describes image-tag and admin-key settings that do not match Compose.
Check the chosen setup path in a teammate dry run and record its outcome before counted sessions.
Instrumentation verification must also finish before the first counted session; neither check has been performed for this test.

## Consent And Data

The consent template and private storage are dependencies on assignment part 4; their readiness has not been verified.
Before a counted session, collect durable consent covering adulthood, recording, sanitized public results, access, withdrawal, and deletion; quotes need separate optional permission.
Name the actual AI and search tools in the consent materials before sessions, including how their saved copies will be removed.
AI inputs may contain public project code, documentation, synthetic examples, and sanitized errors; exclude credentials, private deployment addresses, participant identities, raw session notes, and recordings.
Participants enter test credentials themselves rather than giving them to an AI tool.
Keep raw AI interaction records, when retained as session evidence, in the private text repository; publish only sanitized observations of tool use.
Delete raw data, the private repository and its history, and recordings after grading. Keep sanitized public evidence.

## Evidence Map

| Evidence | Authoritative home |
| --- | --- |
| Design, surveys, unsigned consent template, session instructions | Public `docs/usability-tests/ut-01/`; the supporting files are still pending |
| Sanitized session notes and images | Public `sessions/P-nn.md` and `images/`, created when evidence exists |
| Aggregates, findings, limitations, and conclusions | Public [results.md](results.md), currently empty |
| Signed consent, identity mapping, raw notes, retained AI interaction records, filtered test telemetry, working analysis | Private usability-data repository; creation and access are unverified |
| Session recordings | Team-owned cloud folder; creation and access are unverified |
| Private access instructions and recording-folder link | Moodle wrapper, with the recording-folder link also indexed in the private repository; no raw evidence copied into Moodle |
| Delivery summary and results links | Week 8 public report, linking evidence rather than copying it |

Add the private repository's public link and an authenticated-link-check exclusion once its existence is verified.
Keep deployment addresses, credentials, identities, and recording links out of public files.

See [results.md](results.md) for the eventual findings; the quality verdict belongs in the [testing plan](../../project-plan.md#quality-requirement-testing-plan).
