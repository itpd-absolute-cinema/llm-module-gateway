# Sprint Retrospective — SPRINT-07

## What Went Well

1. The real OpenCode/OpenAI-compatible harness connection was demonstrated end to end, and the customer said the product now looks like a product rather than a prototype.
2. The statistics dashboard and the middleware chain view were shown live, which answered the Week 6 feedback about showing DevOps value instead of only a request to an LLM.
3. The masking and blocking plugins were demonstrated with a recorded backup when the live proxy failed, so the demo continued without losing the customer's attention.
4. The UT-01 record, surveys, consent template, instrumentation plan, and analysis plan were written before the first session, so the usability test design is ready to run in SPRINT-08.

## What Did Not

1. Docker Compose packaging was not done in SPRINT-07. **Root cause:** the team spent the sprint connecting the real harness and building the dashboard, and the packaging task was not rescheduled early enough.
2. The written argument for why the plugin system is better than a set of functions called from a main loop was not produced. **Root cause:** the argument depends on the architecture narrative, which was deferred while the demo work took priority.
3. The UT-01 design review did not happen in the Week 7 meeting; the meeting focused on the working demo. **Root cause:** the agenda allocated the demo first and the design review had no time left.

## Actions

| Action | Owner | Impact | Reason | Carried |
| --- | --- | --- | --- | --- |
| Package the Gateway, Plugin Manager, and CLI in Docker Compose before the next customer demo. | @b4lmor | High | The customer asked for it in Week 6 and it was not done in Week 7. | Yes |
| Write the argument for why the plugin system is better than a set of functions called from a main loop, and use it in the architecture and the demo. | @FallenChromium | High | The customer asked for it in Week 6 and it was not done in Week 7. | Yes |
| Put the UT-01 design review first on the Week 8 agenda, before any demo. | @egozhuk | High | The design review was pushed out by the demo in Week 7 and must happen before the first counted session. | Yes |
| Book the usability sessions and confirm participant attendance one week before each session. | @m1staken | Medium | The recruitment risk is the highest-impact usability risk, and the sessions must run in SPRINT-08. | No |

## Carried Actions

Actions from [SPRINT-06 retrospective](../week-06/sprint-retrospective.md#actions) marked Carried: Yes.

| Action (quoted) | Outcome |
| --- | --- |
| "Package the Gateway, Plugin Manager, and CLI in Docker Compose so the system starts with one command." | Not done — re-listed in ## Actions with Carried: Yes. |
| "Reframe the next demo around a DevOps pain point: plugin validation, failure isolation, or deployment." | Done — the Week 7 demo showed the dashboard, middleware chain, and plugins. |
| "Write down why the plugin system is better than a set of functions called from a main loop, and use it in the architecture and the demo." | Not done — re-listed in ## Actions with Carried: Yes. |
