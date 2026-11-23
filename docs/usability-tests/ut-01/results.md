# UT-01 Results

**Status:** Completed
**Counted sessions:** 3 (P-01, P-02, P-03)

## Per-participant table

| Participant | Task 1 | Task 2 | Task 3 | Task 1 time | Task 2 time | Task 3 time | Total time |
|---|---|---|---|---|---|---|---|
| P-01 | Assisted | Unaided | Assisted | 4:37 | 0:30 | 0:34 | 5:41 |
| P-02 | Unaided | Unaided | Unaided | 2:07 | 0:40 | 0:36 | 3:23 |
| P-03 | Assisted | Unaided | Failed | 6:17 | 0:10 | 0:34 | 7:01 |

SEQ and SUS were not collected. The results report completion, assistance, and time only.

## Aggregate measures

**n = 3.** With three sessions the aggregates are indicative, not conclusive.

- **Task 1 (create a plugin):** 1 of 3 unaided (33%), 2 assisted (67%), 0 failed.
- **Task 2 (enable the plugin):** 3 of 3 unaided (100%), 0 assisted, 0 failed.
- **Task 3 (create a user):** 1 of 3 unaided (33%), 1 assisted (33%), 1 failed (33%).
- **Median Task 1 time:** 4 min 37 s.
- **Median Task 2 time:** 0 min 30 s.
- **Median Task 3 time:** 0 min 34 s.
- **Median total time:** 5 min 41 s.

## Thematic analysis

- **The create-plugin entry point is hard to find without help** (P-01, P-03). P-01 read the documentation first and still needed the navigation hint; P-03 clicked by intuition and took the longest.
- **Documentation and onboarding help, but do not remove the friction** (P-01, P-02). P-02 watched the onboarding and created the plugin unaided; P-01 read the documentation and still needed help.
- **The plugin form is the main pause point** (P-01, P-03). Both participants paused between the code editor and the save control.
- **The enable step is clear** (all three): Task 2 was unaided for every participant, in 10–40 seconds.
- **The Users area is not discoverable** (P-01, P-03). P-01 found it after a hint; P-03 did not find it within the task time. P-02 found it unaided after the onboarding.

## Instrumentation comparison

The telemetry export shows `source=usability-test` events for all three sessions with the participant code. It could not cover the participant's hesitation at the create-plugin form; the observer's notes cover it. No audio was recorded, so no verbal explanation exists; the observer recorded what was visible on screen.

## Limitations

- n = 3, all technically familiar; results may not generalize to non-technical DevOps staff.
- SEQ and SUS were not collected, so perceived ease and overall usability are not measured.
- One facilitator and one observer; no inter-rater reliability check.
- The staging seed is synthetic; a real plugin store may behave differently.

## Conclusion

- **RQ1 (create a plugin unaided?):** Mostly no — 1 of 3 unaided, 2 assisted. The entry point and the form are the blockers.
- **RQ2 (enable the plugin unaided?):** Yes — 3 of 3 unaided.
- **RQ3 (create a user unaided?):** Mostly no — 1 of 3 unaided, 1 assisted, 1 failed. The Users area is the blocker.

**QR/QRT verdict:** Fails. The QR's Response Measure is Task 1 completion and assistance: at least 2 of 3 participants unaided. The observed result is 1 of 3 unaided.

**Product response:** The team commits to making the "New plugin" entry point visible from the plugin list header, adding a one-line hint above the code editor about where the behavior goes, and putting "Users" in the main navigation. The team declines to change the enable flow, which worked for all three.

## Findings

### Finding 1

**Severity:** Major — two of three participants needed help to find the create-plugin entry point, and this blocks the main flow.
**Tasks:** UT-01 Task 1, with UT-01 P-01 and UT-01 P-03.
**Evidence:** [P-01](sessions/P-01.md), [P-03](sessions/P-03.md).
**Recommendation:** Put the "New plugin" action in the plugin list header, next to the list title, not only in the top-right corner.
**Task issue:** #<issue-number>

### Finding 2

**Severity:** Major — two of three participants paused at the plugin form, unsure where the behavior goes.
**Tasks:** UT-01 Task 1, with UT-01 P-01 and UT-01 P-03.
**Evidence:** [P-01](sessions/P-01.md), [P-03](sessions/P-03.md).
**Recommendation:** Add a one-line hint above the code editor: "The behavior goes here; no separate configuration field is needed."
**Task issue:** #<issue-number>
