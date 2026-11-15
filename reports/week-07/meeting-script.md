# Week 7 Customer Meeting Script

**Target:** In one meeting, show the deployed production instance and the UT-01 design on staging, agree the Week 8 usability-test plan and results date, and record every decision as a `DEC-nnn`.

## Roles

- **Facilitator:** @b4lmor
- **Presenter:** @egozhuk
- **Note-taker:** @FallenChromium
- **Timekeeper:** @m1staken

## Agenda

| Time | Item | Owner |
| --- | --- | --- |
| 0:00–0:03 | Permission questions: may we record, may we publish the sanitized transcript, may we share it privately with instructors | @b4lmor |
| 0:03–0:08 | Show the deployed production instance and the core task | @egozhuk |
| 0:08–0:16 | Show the UT-01 record: research questions, participant profile, recruitment target, tasks, surveys, instrumentation and its verification, consent, private repository, recordings folder, session plan, QR/QRT | @FallenChromium |
| 0:16–0:20 | Show the staging deployment: reset seed and test events | @egozhuk |
| 0:20–0:26 | Show the surveys (SUS and SEQ) and the tasks | @FallenChromium |
| 0:26–0:31 | Show the instrumentation and its verification | @egozhuk |
| 0:31–0:35 | Show the consent, the private repository, and the recordings folder | @FallenChromium |
| 0:35–0:40 | Ask the open questions and record the customer's answers as `DEC-nnn` | @b4lmor |
| 0:40–0:44 | Agree the Week 8 execution plan: how sessions are arranged, the results date | @b4lmor |
| 0:44–0:47 | Read back the decisions and action points | @FallenChromium |
| 0:47–0:50 | Close | @b4lmor |

## Questions

1. **Failure policy.** When a required plugin fails or times out, should the Gateway reject the request, skip the plugin, or use a fallback?
2. **Cleanup and diagnostics.** Should outer plugins receive exceptions for cleanup, and which diagnostics may reach the client?
3. **Extension workflow acceptance.** Do the current examples meet the earlier minimum, and can the extension workflow be accepted?
4. **UT-01 design.** Does the test design answer the customer's questions, and are the participant profile, the recruitment target, and the session plan right?
5. **Week 8 results date.** What date must the usability results be ready?

## Working software to show

- The deployed production instance running the core task.
- The UT-01 record on staging.
- The staging reset seed and test events.
- The SUS and SEQ surveys.
- The instrumentation and its verification.
- The consent script, the private repository, and the recordings folder.

## Permission questions

- May we record this meeting?
- May we publish the sanitized transcript in the repository?
- May we share the transcript privately with the instructors if publication is refused?

## Close

- Read back each `DEC-nnn` and each action point with owner and due week.
- Confirm the Week 8 results date.
- Confirm the UT-01 design as agreed or with changes.