# Sprint 04 retrospective

LLM Gateway, Team 1 — Absolute cinema.
This retrospective records the team's written discussion after the sprint, not a separate customer meeting.
The [sprint plan](../../docs/project-plan.md#sprint-04) records 6 planned points and 0 completed story points; the CLI story remains open and customer review is deferred.

## What Went Well

| What went well |
| --- |
| We delivered the plugin-management API with automated checks. |
| We settled restart-only configuration before adding more plugin lifecycle complexity. |
| We combined parallel documentation work while preserving the teammate's architecture and reducing unnecessary rewrites. |

## What Did Not

| Problem | Root cause |
| --- | --- |
| Customer validation moved to Week 5, while preparation and integration had too little headroom before the intended call. | The meeting was postponed for reasons outside our control; our own delivery cadence also left too little buffer for integration and rehearsal. |
| Combining the API and documentation work took avoidable rework. | Dependent work progressed in parallel against changing interfaces and overlapping records; the initial AI-assisted merge also rewrote more than necessary. |

## Actions

| Action | Owner | Impact | Reason | Carried |
| --- | --- | --- | --- | --- |
| Set an internal delivery cutoff at least one day before the next customer call; reserve the remaining time for integration and fixes. | b4lmor | High | We need preparation time that is separate from the customer meeting slot. | Yes |
| Run the combined demo during that buffer and review blockers with b4lmor before the call, rather than discovering them while presenting. | FallenChromium | High | A cutoff only helps if we use the buffer to check the integrated result. | Yes |
| Order dependent work explicitly: agree and merge an interface or document change before starting the work that consumes it; keep independent tasks parallel. | b4lmor | High | Stable inputs should reduce incompatible implementations and overlapping documentation edits. | Yes |

## Carried Actions

None: SPRINT-04 is the first sprint with a retrospective, so there is no previous retrospective to carry actions from.
