# Risk Register

**Project:** LLM Gateway  
**Status:** Draft – risk ratings and responses require team review.

## Risk Summary

| ID | Risk | Rating | Trigger | Window | Status |
| --- | --- | --- | --- | --- | --- |
| RISK-01 | Plugin development is more complex than expected. | High | The example plugin cannot be added and executed without changing the Gateway core. | SPRINT-03 to SPRINT-09 | Open |
| RISK-02 | Gateway and Interpreter integration is delayed by technical issues. | High | The automated basic request-processing test fails or the Gateway cannot start locally. | SPRINT-03 to SPRINT-04 | Open |
| RISK-03 | Plugin failures interrupt request processing unexpectedly. | High | An injected plugin failure terminates the Gateway or violates the documented error policy. | SPRINT-04 to SPRINT-09 | Open |
| RISK-04 | The CLI and end-to-end workflow are not completed in time. | Medium | The CLI cannot send a request and return a response by the end of its planned sprint. | SPRINT-05 to SPRINT-08 | Open |
| RISK-05 | Limited team capacity delays planned work. | Medium | An issue is blocked or remains incomplete at a sprint review because its owner lacks available time or required knowledge. | SPRINT-03 to SPRINT-09 | Open |

## RISK-01 – Plugin Development Is Too Complex

**Risk statement:** Because the plugin interface and discovery mechanism may require knowledge of internal Gateway details, developers may be unable to add a plugin without changing the core, preventing the project from meeting its primary extensibility goal.

**Rating:** High – preliminary.

**Trigger:** The example plugin cannot be added and executed by following the documented instructions without modifying Gateway core code.

**Window:** SPRINT-03 to SPRINT-09; check during every Friday review.

**Mitigation:** Define a minimal plugin contract, provide a working example, and add an automated test that discovers and executes the example plugin.

**Contingency:** Reduce the initial plugin interface to the smallest working contract and defer optional plugin features until the example plugin passes the acceptance test.

## RISK-02 – Gateway and Interpreter Integration Is Delayed

**Risk statement:** Because the Gateway and Python Interpreter may have incompatible interfaces or runtime assumptions, their integration may fail, preventing delivery of the Minimum Usable Product by 18 October 2026.

**Rating:** High – preliminary.

**Trigger:** The Gateway cannot start locally or the automated basic request-processing test cannot pass.

**Window:** SPRINT-03 to SPRINT-04; check at each development integration and Friday review.

**Mitigation:** Define the component interface early, integrate a minimal vertical slice, and automate a test covering request handling and example plugin execution.

**Contingency:** Reduce the MUP to the smallest demonstrable request-processing path between the Gateway and Interpreter, deferring non-essential features.

## RISK-03 – Plugin Failures Interrupt Request Processing

**Risk statement:** Because plugin execution may raise exceptions or fail unexpectedly, a plugin failure may terminate the Gateway or disrupt the plugin chain, undermining the system's reliability goal.

**Rating:** High – preliminary.

**Trigger:** An automated test injecting a controlled plugin failure shows an unhandled Gateway failure or behavior inconsistent with the documented error policy.

**Window:** SPRINT-04 to SPRINT-09; check whenever plugin execution or error handling changes.

**Mitigation:** Define the expected error-handling policy, isolate plugin execution failures, and add automated tests for the expected chain behavior and Gateway availability.

**Contingency:** Disable the failing plugin or restrict the MVP to the tested error-handling behavior until the failure can be handled safely and predictably.

## RISK-04 – CLI and End-to-End Workflow Are Delayed

**Risk statement:** Because the CLI depends on a working Gateway API and a stable request-response contract, integration problems may prevent the CLI from completing a request workflow, leaving the MVP without its intended terminal interface.

**Rating:** Medium – preliminary.

**Trigger:** An automated integration test cannot invoke the CLI, send a request through the Gateway, and verify the returned response by the end of the sprint in which the CLI is planned.

**Window:** SPRINT-05 to SPRINT-08; check during implementation and every Friday review.

**Mitigation:** Keep the CLI scope minimal, agree on the request-response contract, and test it against a running Gateway.

**Contingency:** Provide a minimal command that sends one request and prints its response, deferring optional CLI commands and conveniences.

## RISK-05 – Limited Team Capacity Delays Planned Work

**Risk statement:** Because team members contribute according to their availability, competencies, and willingness to take on tasks, critical work may remain unowned or blocked, delaying milestone delivery.

**Rating:** Medium – preliminary.

**Trigger:** A planned issue is blocked or incomplete at a sprint review because no available team member can progress it or required knowledge is missing.

**Window:** SPRINT-03 to SPRINT-09; check during every Friday review.

**Mitigation:** Review issue ownership and blockers weekly, share implementation knowledge, keep tasks small, and identify a backup contributor for critical work where possible.

**Contingency:** Reassign available team capacity to the critical path and defer optional features that are not required by the agreed Threshold of Success.

## Weekly Risk Review and Settling

The team reviews this register every Friday.

For each risk, the team:

1. Checks whether the trigger has occurred and records relevant evidence.
2. Reassesses the rating if the likelihood or impact has changed.
3. Checks mitigation progress and assigns follow-up issues where needed.
4. Decides whether the contingency must be activated.
5. Records the decision, responsible issue owner, and next review action.

A risk is not considered resolved merely because a mitigation has been planned. The team checks whether the mitigation has worked and records the evidence during the review.

Risk ratings and responses are provisional until reviewed by the team. No customer approval or decision identifier is implied by this register.
