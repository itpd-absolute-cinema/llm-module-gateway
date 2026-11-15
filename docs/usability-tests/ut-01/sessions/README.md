# UT-01 Session Guide

## Moderator Setup

Use an isolated staging instance on the university network with the stub LLM provider. Check that the test account can manage plugins, the terminal is signed in, and the recording excludes authentication and setup. Give the participant the CLI reference, the supplied `mask_phone.py` fixture from `code/plugin-manager/examples/`, and a synthetic request client. Use the default `[PHONE]` mask. Before each session, restore the same registry seed. Make sure the target filter is absent for T1 and that one inactive fixture is present. For T2, prepare the phone filter in the active state and keep the inactive fixture. Reset with the participant away from the terminal.

In a shell outside the recording, set `UT01_STAGING_URL` to the private staging endpoint, then run the following from the repository root.

```bash
export LLM_GATEWAY_URL="$UT01_STAGING_URL"
export UT01_SESSION_ID="$(python3 -c 'import uuid; print(uuid.uuid4())')"
export UT01_EVENT_LOG="$(mktemp "${TMPDIR:-/tmp}/ut01.XXXXXXXX")"
repo_path="$PWD"
gw() { python3 "$repo_path/code/cli/usability_gateway_cli.py" "$@"; }
```

Write the UUID beside the participant code in the private sheet. Confirm the file was created before beginning. Use `gw` for the documented plugin commands and `gw probe` as the synthetic request client. Let the participant know how to send the sample request. Leave plugin command discovery to the participant. Check the baseline before the session with a separate preflight log. Use the normal CLI for moderator reset calls so the session log contains only participant actions. Move the session file to approved private storage as part of the handoff.

## Facilitator Script

1. Welcome. “We are testing the Gateway. Please work as you normally would and tell us what you notice.”
2. Confirm eligibility and durable consent. Check permission for recording and sanitized publication. Start recording only after consent. Stop if consent is missing, withdrawn, or the recording fails.
3. Ask the [pre-test questions](../surveys.md#pre-test). Then explain think-aloud. “Say what you are looking for, what you expect, and what surprises you.”
4. Read each [task scenario](../README.md#the-tasks) verbatim, start the timer, and follow the help rule in the test record. If the participant goes quiet, ask “What are you thinking about?” Log requested hints and their timestamps. Stop at success, the time limit, or the participant's request. If the environment fails, stop the session, record the reason, and exclude it from counted sessions.
5. Ask the [SEQ](../surveys.md#after-each-task) after each task. Prepare the next starting state away from the participant. Preparation time is outside the task timer.
6. After the final task, ask the [SUS and open-ended questions](../surveys.md#after-all-tasks).
7. Debrief, thank the participant, remind them how to withdraw, and stop recording. Transfer the recording and notes to the agreed private storage.

## Private Session Sheet Template

| Field | Entry |
| --- | --- |
| Participant code and session UUID | |
| Date, facilitator, observer, and staging version | |
| Eligibility, durable consent, recording confirmation | |
| Pre-test answers | |
| Ten SUS ratings and computed score | |
| Open-ended answers and debrief notes | |
| Private recording and telemetry references | |
| Session counted or excluded, with reason | |

| Task | Start / stop | Elapsed | Outcome | Hint and time | Think-aloud, errors, or friction | SEQ |
| --- | --- | --- | --- | --- | --- | --- |
| T1 | | | | | | |
| T2 | | | | | | |
| T3 | | | | | | |

## Rehearsal and Verification

| Run | Sprint | What to record | Status |
| --- | --- | --- | --- |
| Teammate rehearsal | SPRINT-07 | Check scenario wording, limits, timing, the seed reset, and the JSONL records. Log changes needed. Exclude this run from evidence and participant counts. | Pending |
| Staging instrumentation verification | SPRINT-08 | Before the first counted session, run T1 with a teammate. Check the masked reply and confirm that the expected records reached the JSONL file under the recorded session UUID. | Pending |
