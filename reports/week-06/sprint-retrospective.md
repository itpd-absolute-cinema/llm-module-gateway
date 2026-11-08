# Sprint Retrospective — SPRINT-06

## What Went Well

1. The one-line plugin installation was demonstrated live in the customer meeting, and the CLI path worked on the first attempt after the version mix-up was resolved.
2. The Gateway → Plugin Manager → LLM provider flow was shown end to end with a real DeepSeek API key, which made the request path visible without a stub.
3. The analytics and observability ADRs were written and accepted in the same sprint they were planned, so the architecture document did not drift from the code.
4. The threshold review recorded why no goal moved, which is a complete review rather than an empty one.
5. The plugin loading path was shown in the Gateway source during the meeting, so the customer could see where the architecture decision lives.

## What Did Not

1. The customer said the demo did not show why a DevOps engineer would choose this product over writing their own functions. **Root cause:** the demo showed a request reaching an LLM, not a DevOps pain point such as plugin validation, failure isolation, or deployment.
2. The system was not packaged in Docker Compose, so the customer had to watch the team fix path and version issues live. **Root cause:** the team treated packaging as a later task and did not budget for it in the sprint.
3. The customer could not see the plugin loading and execution path immediately when asked; the team had to open several files. **Root cause:** the architecture was known to the team but not prepared as a demo artifact.

## Actions

| Action | Reason | Carried |
| --- | --- | --- |
| Package the Gateway, Plugin Manager, and CLI in Docker Compose so the system starts with one command. | The customer asked for it directly, and version/path issues were visible during the demo. | Yes |
| Reframe the next demo around a DevOps pain point: plugin validation, failure isolation, or deployment. | The customer said the current demo does not show product value for a DevOps engineer. | Yes |
| Write down why the plugin system is better than a set of functions called from a main loop, and use it in the architecture and the demo. | The customer asked for this argument explicitly. | Yes |
| Prepare a one-slide map of the plugin loading and execution path before each customer demo. | The team had to open several files to answer an architecture question during the meeting. | No |

## Carried Actions

Actions from [SPRINT-05 retrospective](../week-05/sprint-retrospective.md#actions) marked Carried: Yes.

| Action (quoted) | Outcome |
| --- | --- |
| "[action from SPRINT-05 that was marked Carried: Yes]" | Done / Not done / Dropped |
| "[action from SPRINT-05 that was marked Carried: Yes]" | Done / Not done / Dropped |
