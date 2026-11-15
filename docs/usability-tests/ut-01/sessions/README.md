# UT-01 Session Guide

## Moderator Setup

Use the [test record](../README.md) as the source for eligibility, scenarios, starting states, limits, and help rules.
Book about 50 minutes per participant, with time between bookings to reset the environment.
Confirm the provisional team roles and private storage access before booking.

Before the participant arrives, check Docker and Compose, Python, staging connectivity, the stub provider, and the supplied Keycloak account and role.
Use the same commit, tool versions, image-cache state, and AI configuration for everyone.
Prepare the fresh checkout with the gateway stopped and unconfigured for T1.
Clear earlier CLI tokens, browser sessions, and AI history; do not ask the participant to use a personal account.

The recording must cover setup and normal sign-in behavior, since these are part of the tasks.
Mask secret fields and keep credentials, tokens, private addresses, and unrelated windows out of the capture; record any necessary concealment in the private notes.
Do not omit the whole setup or login flow from the evidence.
Fill the consent's tool and contact fields, check how saved tool copies will be deleted, and collect durable consent before recording.

Configure the private staging endpoint and the supplied identity-provider settings outside the recording.
Then prepare the test client from the repository root:

```bash
export LLM_GATEWAY_URL="$UT01_STAGING_URL"
export UT01_SESSION_ID="$(python3 -c 'import uuid; print(uuid.uuid4())')"
export UT01_EVENT_LOG="$(mktemp "${TMPDIR:-/tmp}/ut01.XXXXXXXX")"
repo_path="$PWD"
gw() { python3 "$repo_path/code/cli/usability_gateway_cli.py" "$@"; }
```

`UT01_STAGING_URL` must be the private staging URL, never production.
Write the UUID beside the participant code in the private sheet and confirm the file exists.
Give everyone the same CLI reference: `gw` runs the documented commands and `gw probe` sends `Call +1 202 555 0100` to the stub provider.
The expected reply is `[stub] Call +1 202 555 0100` without the filter and `[stub] Call [PHONE]` with it.
Do not teach the enable or disable commands.

Use the normal CLI for moderator resets, with a separate preflight log for checks; those calls must not enter the participant event log.
After T1's SEQ, prepare the running gateway with `mask_phone` registered but disabled, default options, and no other filters.
The participant starts T2 signed out and uses the supplied account to sign in.
After T2's SEQ, ensure the same filter is enabled for T3, even if T2 failed.
Make resets away from the participant, record them, and leave the earlier task's result unchanged.
Verify the exact reset procedure in the teammate pilot; it is not yet staging-verified.

## Facilitator Script

1. Welcome: “We are testing the Gateway. Work as you normally would and tell us what you notice. You may use the documentation, web search, and the supplied AI tool.”
2. Confirm eligibility and [durable consent](../consent.md), including recording and sanitized publication. Start recording after consent. Missing or withdrawn consent stops the session.
3. Ask the [pre-test](../surveys.md#pre-test), then explain think-aloud: “Say what you are looking for, what you expect, and what surprises you.”
4. Read each [task scenario](../README.md#the-tasks) verbatim. Start timing when the starting state is ready and the participant begins. Limits are T1: 20 minutes, T2: 10 minutes, T3: 5 minutes. Stop at the demonstrated end state, giving up, or the limit. Do not pause for reading, AI responses, downloads, or retries.
5. Observe without coaching. You may repeat the scenario or ask “What are you thinking about?” Record any human hints that occur and their times. Record web-search and AI use separately, including whether AI advised or executed actions.
6. Ask the [SEQ](../surveys.md#after-each-task) after every attempt, including a failure, before preparing the next task. Use a fresh starting state as specified above; reset time is outside task time.
7. After T3, collect all ten [SUS responses and open-ended answers](../surveys.md#after-all-tasks). Do not show the participant the pass threshold or suggest a rating.
8. Debrief, remind the participant how to withdraw, and stop recording. Transfer raw notes and the JSONL export to the private repository and the recording to the cloud folder. Clear saved workstation and tool history after preserving required evidence privately.

Stop the session if the participant withdraws, required recording fails, or a critical or security-sensitive problem occurs.
Record the reason; an incomplete session does not count.
A product error or confusing documentation is a task outcome, not an excuse to exclude a completed session.
An unrelated infrastructure failure that prevents the study is recorded as a broken session and replaced under the next code.

## Private Session Sheet Template

| Field | Entry |
| --- | --- |
| Participant code and session UUID | |
| Date, facilitator, observer, staging commit, and device | |
| Eligibility, durable consent, recording confirmation | |
| Docker, Compose, gateway, and AI familiarity | |
| AI tool/model, configuration, and permissions | |
| Pre-test answers | |
| Ten SUS ratings and computed score | |
| Open-ended answers and debrief notes | |
| Private recording, telemetry, and retained AI-interaction references | |
| Task resets or recording concealments, with reason | |
| Session counted or excluded, with reason | |

| Task | Start / stop (UTC) | Elapsed (s) | Outcome | Human help and time | Observations | SEQ |
| --- | --- | --- | --- | --- | --- | --- |
| T1 | | | | | | |
| T2 | | | | | | |
| T3 | | | | | | |

| Task | Web search used | AI used | AI mode: advice or execution | Helpful or misleading output; wrapper bypasses |
| --- | --- | --- | --- | --- |
| T1 | | | | |
| T2 | | | | |
| T3 | | | | |

Use UTC task windows and the UUID to match client events to observations.
Events outside the wrapper, Docker setup, login, and AI use need observation; client request duration does not measure task duration.
A successful API call alone does not establish the task's full end state.

## Rehearsal and Verification

| Run | Sprint | What to record | Status |
| --- | --- | --- | --- |
| Teammate rehearsal | SPRINT-07 | Check wording, the 20/10/5-minute limits, consent, surveys, recording, auth, tools, and resets. Record fixes; exclude the run from participant results. | Pending |
| Staging instrumentation verification | SPRINT-08 | Before the first counted session, run T1–T3. Confirm unmasked/masked/unmasked probes, enable/disable calls, the correct UUID and task windows, and export to private storage. Record commit, verifier, date, and outcome. | Pending |
