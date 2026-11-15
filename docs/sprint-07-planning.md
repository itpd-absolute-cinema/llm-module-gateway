# Sprint 07 Planning Draft

The [project plan](project-plan.md#sprint-07) gives the sprint goals and dates. On 15 November, the SPRINT-07 and SPRINT-08 milestones were empty. The tasks below are drafts for those milestones. The priorities and owners below are proposed. When scheduling a task in GitHub, use its Responsible and Accountable owners for the corresponding project fields and add both as assignees.

The test covers the existing stories for [uploading a plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/17), [inspecting the configuration](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/19), and [switching plugins on and off](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/22). These stories are currently in SPRINT-05.

The plan assumes the first and last counted sessions are in SPRINT-08. If all counted sessions move to SPRINT-07, move the session and verification issues there. If the sessions span both sprints, schedule verification before the first session and the session issue in the sprint of the last session. Results and the quality verdict stay in SPRINT-08.

## Sprint 07 Task Drafts

### Write the UT-01 record

**Priority:** High

**Responsible:** `@egozhuk`

**Accountable:** `@m1staken`

#### Requirements

[FR-01](functional-requirements/fr/FR-01.md), [FR-03](functional-requirements/fr/FR-03.md), [FR-06](functional-requirements/fr/FR-06.md)

#### Acceptance criteria

- [ ] AC-01. The usability index lists UT-01 as Planned with results Pending. The record links the design sprint, run sprint, and usability quality requirement.
- [ ] AC-02. The record states the goal and two or three research questions, includes the required sections and evidence map, and links the companion files and results stub.

### Prepare the session surveys

**Priority:** High

**Responsible:** `@m1staken`

**Accountable:** `@egozhuk`

#### Requirements

[FR-01](functional-requirements/fr/FR-01.md), [FR-06](functional-requirements/fr/FR-06.md)

#### Acceptance criteria

- [ ] AC-01. The survey includes a pre-test, a seven-point SEQ after each task, all ten standard SUS items in order with the five-point scale and scoring formula, and open-ended post-test questions.

### Write the tasks and facilitator protocol

**Priority:** High

**Responsible:** `@egozhuk`

**Accountable:** `@m1staken`

#### Requirements

[FR-01](functional-requirements/fr/FR-01.md), [FR-02](functional-requirements/fr/FR-02.md), [FR-03](functional-requirements/fr/FR-03.md), [FR-04](functional-requirements/fr/FR-04.md), [FR-06](functional-requirements/fr/FR-06.md)

#### Acceptance criteria

- [ ] AC-01. One main task and two secondary tasks have a fixed order, neutral scenario wording, starting state, target outcome, time limit, help rule, and criteria for Unaided, Assisted, and Failed.
- [ ] AC-02. The session guide includes consent and recording checks, think-aloud instructions, a sheet for timing and observations, survey timing, debrief, and a stop rule. The record names the three session roles by GitHub username.

### Design the UT-01 instrumentation

**Priority:** High

**Responsible:** `@FallenChromium`

**Accountable:** `@b4lmor`

#### Requirements

[FR-010](functional-requirements/fr/FR-010.md), [ADR-007](architecture/adr/ADR-007.md), [ADR-008](architecture/adr/ADR-008.md)

#### Acceptance criteria

- [ ] AC-01. The record identifies what is observed, which Week 6 events are reused, which records are added, where they are emitted and stored, how they are matched to a session, and what still requires observation.

### Implement the staging instrumentation

**Priority:** High

**Responsible:** `@b4lmor`

**Accountable:** `@FallenChromium`

#### Requirements

[FR-010](functional-requirements/fr/FR-010.md), [ADR-007](architecture/adr/ADR-007.md), [ADR-008](architecture/adr/ADR-008.md)

#### Acceptance criteria

- [ ] AC-01. A staged run of the main task generates the documented records and lets the team associate them with a session without collecting participant identities.
- [ ] AC-02. Any events sent through the analytics pipeline are documented in the event table and carry `source=usability-test`. Product metrics exclude smoke and usability-test traffic.

### Prepare consent and private text storage

**Priority:** High

**Responsible:** `@egozhuk`

**Accountable:** `@m1staken`

#### Requirements

None: repository upkeep

#### Acceptance criteria

- [ ] AC-01. The public template covers age, recording, sanitized publication, optional quotes, private access, tools, withdrawal, and deletion after grading. The signature line is blank.
- [ ] AC-02. The private usability-data repository has places for consent, the identity mapping, raw notes, analysis, and filtered telemetry. Team and instructor access is checked.
- [ ] AC-03. The public record and Week 7 report link the private repository. The link checker excludes its URL with a reason.

### Prepare the recordings folder

**Priority:** High

**Responsible:** `@m1staken`

**Accountable:** `@egozhuk`

#### Requirements

None: repository upkeep

#### Acceptance criteria

- [ ] AC-01. The team owns the cloud folder and confirms instructor access. The folder URL and access instructions are in the private repository and ready for the Moodle submission.

### Define the usability quality requirement and its manual test

**Priority:** High

**Responsible:** `@FallenChromium`

**Accountable:** `@m1staken`

#### Requirements

The Interaction Capability quality requirement created in this task. Add its permanent ID and link to the issue when allocated.

#### Acceptance criteria

- [ ] AC-01. The quality requirement defines the measure and pass threshold before sessions begin. Its manual test links UT-01 and explains how to determine the verdict.
- [ ] AC-02. The testing plan assigns the check to SPRINT-08 with status Not verified, Checked None, and Evidence None.

### Plan recruitment and sessions

**Priority:** High

**Responsible:** `@m1staken`

**Accountable:** `@b4lmor`

#### Requirements

[FR-01](functional-requirements/fr/FR-01.md), [FR-06](functional-requirements/fr/FR-06.md)

#### Acceptance criteria

- [ ] AC-01. The record defines the target participant profile and a recruitment plan for five representative adults, with at least three counted sessions. It defines how codes and replacements are handled.
- [ ] AC-02. The session plan explains how the team will arrange in-person sessions on university-network staging, reset the synthetic data, and check recording consent.

### Rehearse UT-01 with a teammate

**Priority:** High

**Responsible:** `@egozhuk`

**Accountable:** `@b4lmor`

#### Requirements

[FR-01](functional-requirements/fr/FR-01.md), [FR-06](functional-requirements/fr/FR-06.md), [FR-010](functional-requirements/fr/FR-010.md)

#### Acceptance criteria

- [ ] AC-01. A teammate rehearses the tasks on staging. The team checks the wording, time limits, and instrumentation, records any fixes, and marks the rehearsal as excluded from the study results.

### Write the analysis and triage plan

**Priority:** High

**Responsible:** `@m1staken`

**Accountable:** `@FallenChromium`

#### Requirements

The usability quality requirement defined above and [FR-010](functional-requirements/fr/FR-010.md).

#### Acceptance criteria

- [ ] AC-01. The plan defines the participant table, completion and assistance, main-task median time, median SEQ by task, SUS mean and median, the QR measure, and how to compare observations with telemetry.
- [ ] AC-02. The plan defines themes, severity, limitations, and triage. The results stub has headings and the finding template ready for Week 8.

## Sprint 08 Task Drafts

The verification item is due before the first counted session and must be completed by the start of SPRINT-09.

| Task | Priority | Responsible | Accountable | Requirements | Acceptance criteria |
| --- | --- | --- | --- | --- | --- |
| Verify staging instrumentation | High | `@FallenChromium` | `@b4lmor` | [FR-010](functional-requirements/fr/FR-010.md) | AC-01. A teammate runs the main task before the first counted session. The team records that the expected data reached the documented store and could be tied to that run. |
| Conduct UT-01 sessions | High | `@egozhuk` | `@m1staken` | [FR-01](functional-requirements/fr/FR-01.md), [FR-06](functional-requirements/fr/FR-06.md) | AC-01. Run at least three eligible, consented, recorded sessions with the fixed protocol. Capture each task outcome, assistance, elapsed time, SEQ, and post-test responses. |
| Prepare sanitized notes | High | `@m1staken` | `@egozhuk` | None: repository upkeep | AC-01. Remove identities and identifying details from any evidence used publicly. Keep raw notes and the filtered telemetry export in private storage. |
| Analyze and publish results and findings | High | `@m1staken` | `@FallenChromium` | Usability quality requirement, [FR-010](functional-requirements/fr/FR-010.md) | AC-01. Publish the planned aggregates, findings with evidence and severity, and limitations. State the sample size and where telemetry could and could not corroborate observations. |
| Triage usability findings | High | `@FallenChromium` | `@egozhuk` | Usability quality requirement and the FR or ADR affected by each finding | AC-01. Record a decision for every finding. When there is a significant finding, put at least one linked task in SPRINT-08 with the relevant story and acceptance criterion for a behavior change. |
| Record the usability QR/QRT verdict | High | `@FallenChromium` | `@m1staken` | Usability quality requirement | AC-01. Apply the agreed threshold to the results and update the testing plan with the verdict and evidence. If the study cannot finish, document the reason and handle it in the testing plan. |
| Cut REL-08 | High | `@b4lmor` | `@FallenChromium` | None: repository upkeep | AC-01. Publish the tagged release and its upgrade note, update the changelog and release record, and close the milestone after moving open work. See [issue #54](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/54) for the previous release task format. |

## Refined Product Backlog

These are additional product work candidates. Leave their milestone empty during refinement. The sprint column shows the planning horizon.

| Sprint | Candidate | Priority | Requirements | Acceptance criteria and dependencies |
| --- | --- | --- | --- | --- |
| SPRINT-08 | Verify the plugin lifecycle through the CLI | Medium | [FR-01](functional-requirements/fr/FR-01.md), [FR-02](functional-requirements/fr/FR-02.md), [FR-03](functional-requirements/fr/FR-03.md), [FR-04](functional-requirements/fr/FR-04.md) | AC-01. Starting with a clean Compose deployment, an end-to-end check uploads the sample plugin, verifies its initial state, enables it, disables it, and checks the state after each change. Requires a reproducible Compose deployment. |
| SPRINT-08 | Verify plugin effects across the request path | Medium | [FR-06](functional-requirements/fr/FR-06.md), [FR-07](functional-requirements/fr/FR-07.md) | AC-01. An end-to-end check uses a fixed synthetic input and a controlled provider to verify the exact request received after filtering and the baseline when the filter is disabled. |
| SPRINT-09 | Implement and verify the required plugin timeout policy | High | [QR-02](quality-requirements/qr/QR-02.md), [FR-06](functional-requirements/fr/FR-06.md) | AC-01. A controlled timeout produces the agreed request outcome and records a diagnostic. AC-02. A later request can still complete. Depends on the customer's decision on timeout and cleanup behavior. |
| SPRINT-09 | Verify the extension workflow against TOS-02 | Medium | [FR-01](functional-requirements/fr/FR-01.md), [FR-06](functional-requirements/fr/FR-06.md), [Threshold of Success](threshold-of-success.md) | AC-01. From a clean deploy, follow the published guide to add a sample extension, show its effect on a synthetic request, and link the evidence. Requires the documented deployment workflow. |
