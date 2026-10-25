# Project Plan

**Project:** LLM Gateway  
**Repository:** <https://github.com/itpd-absolute-cinema/llm-module-gateway>\
**Target completion:** 4 December 2026  
**Status:** Draft – proposed sprint deadlines require confirmation against the course calendar.

## Milestones

### MLS-03 – Deliver the Minimum Usable Product (MUP)

**Due:** Oct 25 2026

**Deliverable:** A working Gateway and Python Interpreter that demonstrate the minimum request-processing flow.

**Acceptance criteria:**

- The Gateway accepts an API request.
- The Python Interpreter can discover and execute an example Python plugin.
- The Gateway and Interpreter can be run locally using the documented development workflow.
- An automated test verifies the basic request-processing flow.

**Threshold of Success:** TOS-01 and TOS-02 in [Threshold of Success](threshold-of-success.md).

**Sprints:** SPRINT-03

**Description:**

Deliver the smallest usable increment of the LLM Gateway: a request-handling component and a Python Interpreter capable of executing an example plugin. The implementation should establish the component boundary and demonstrate that plugin functionality can be added without modifying the Gateway core.

Dynamic plugin loading without a restart and horizontal scaling are not required for this milestone. They remain potential future improvements unless explicitly included in an agreed goal.

## Sprints

### SPRINT-03 – Minimum Usable Product

**Goal:** Define the MUP candidate, including its core functionality, architecture, and technology stack, without implementing the complete product.

**Due:** 18 October 2026

**Sprint Backlog:** [SPRINT-03 issues](https://github.com/itpd-absolute-cinema/llm-module-gateway)

## SPRINT-04

**Goal:** Establish the first working integration between the CLI and the Gateway through an HTTP API, and enable the integration of the first plugins into the request-processing pipeline.

**Due:** 25 October 2026

**Sprint Backlog:** [SPRINT-04 milestone](https://github.com/itpd-absolute-cinema/llm-module-gateway/milestone/2)

**Planned Velocity:** 6

**Actual Velocity:** 6

#### REL-04

TODO

### SPRINT-05 – Client-to-Gateway Request Flow (Provisional)

**Goal:** Enable an OpenAI-compatible client to send requests through the Gateway and receive responses processed by the configured plugin chain.

**Due:** 31 October 2026

#### REL-05

### SPRINT-06 – Plugin Error Handling (Provisional)

**Goal:** Provisional – define and test the plugin error-handling policy so failures are handled predictably without unintended Gateway termination.

**Due:** 7 November 2026

#### REL-06

### SPRINT-07 – Local Deployment and Documentation (Provisional)

**Goal:** Provisional – make the documented local Docker workflow reproducible and improve the developer instructions for running the Gateway and adding plugins.

**Due:** 14 November 2026

#### REL-07

### SPRINT-08 – End-to-End Verification (Provisional)

**Goal:** Provisional – strengthen automated end-to-end verification of the Gateway, Interpreter, CLI, and plugin-chain behavior against the Threshold of Success.

**Due:** 21 November 2026

#### REL-08

### SPRINT-09 – MVP Completion and Acceptance Evidence (Provisional)

**Goal:** Provisional – address remaining MVP gaps, verify the agreed Threshold of Success criteria, and prepare the final demonstration and documentation.

**Due:** 28 November 2026

#### REL-09

## Progress Monitoring

**Weekly review:** Friday.

**Who checks:** The owners of the relevant issues check implementation progress and evidence before the weekly review. The team reviews the overall status, blockers, risks, and next actions together.

**Progress visibility:**

- [GitHub Project – Table view](https://github.com/orgs/itpd-absolute-cinema/projects/1) – issue status, ownership, and overall progress.
- [GitHub repository](https://github.com/itpd-absolute-cinema/llm-module-gateway) – code, pull requests, tests, and documentation.
- GitHub sprint milestones – sprint scope, due dates, and completion status.

**Customer communication:** Artem L. is responsible for customer meetings.

**Risk review:** The team reads and reviews the [Risk Register](risks.md) during every Friday review. For each risk, the team checks whether its trigger has occurred, whether its rating remains appropriate, whether mitigation is progressing, and whether its contingency needs to be activated. The team records decisions and follow-up actions in the relevant issues.

**Sprint planning:** SPRINT-03 and SPRINT-04 have concrete goals in this draft. The goals for SPRINT-05 through SPRINT-09 are provisional and must be revisited before each sprint is planned.

**Release records:** Each `REL-nn` section remains empty until the corresponding release has actually been published. No release is claimed by this draft.
