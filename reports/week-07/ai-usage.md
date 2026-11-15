# AI Usage

Codex read the local course assignment, requirements, and usability guide to help prepare assignment 7 parts 5–7.
The user selected gateway setup and a first response as the main journey, and a main-task median ease rating of at least 5/7 as the quality criterion.
The user selected a prepared staging machine with Docker installed, a fresh checkout, and the gateway stopped as the starting point.
The user selected developers who have used Docker but are new to this gateway as the participant profile.
The user selected enabling a supplied filter and disabling it again, with verification of both effects, as the secondary tasks.
The user plans to supply staging Keycloak and a test account; Codex recorded them as team-provided prerequisites, with normal CLI sign-in included in the first secondary task.
The user selected a 20-minute setup limit; the draft allocates 10 minutes to enabling the filter and 5 to disabling it, with roughly 50 minutes for the whole session as proposed in the discussion.
The user allowed documentation, web search, and AI assistance during participants' tasks. Codex added separate tool-use records and limited the conclusions to that combined workflow; participant AI use remains future work, not a claim about sessions already held.
Codex drafted UT-01, QR-07 and its manual test, the analysis and evidence map, the empty results template, and documentation links.

Codex translated that profile into screening criteria and proposed recruitment channels and a booking timetable; no invitations were sent and no sessions were booked.
Codex inspected the supplied phone filter and plugin controls to define the expected responses and task resets; no staging run was performed.
Codex integrated PR #83's parts 1–4 with these choices: retained its surveys, provisional roles, private-data link, and client event recorder; changed its plugin-upload task to setup; and reconciled the session guide, consent, timing, AI rules, and probe text.
The private repository was verified to exist with private visibility; participant records were not opened. Instructor access and the recordings folder remain unverified.
Five offline recorder tests passed, covering task probe effects, enable/disable events, failed requests, log contents, and rejecting a host outside the configured staging URL. Markdown validation and the strict documentation build also passed; these checks do not verify university staging.
At the user's request, local link validation used `npx markdown-link-check@3.15.0` for `docs/`, the Week 7 report, AGENTS, the changelog, and the CLI guide, with the authenticated private repository excluded. It passed; the repository's GitHub workflow still uses Lychee.
The provisional roles need confirmation, consent needs the actual tool/contact details, and staging instrumentation and resets remain unverified; the draft does not claim readiness or results.
No participant material, recordings, or raw private notes were supplied to the agent.
Human review of the draft remains pending.

# AI Usage

## Overview

AI tools were used as supporting tools throughout the development of this project. They assisted the team with software design, implementation planning, documentation, code development, and problem-solving.

The AI tools used during the project included **ChatGPT, DeepSeek, Claude**, and other AI-powered assistants where appropriate.

## Areas of AI Assistance

AI assistance was used in the following areas:

- **Requirements analysis:** clarifying task requirements, identifying constraints, and breaking down larger objectives into smaller, manageable tasks.
- **Architecture and design:** exploring possible architectural approaches, evaluating implementation options, and discussing component responsibilities and interfaces.
- **Implementation support:** generating code examples, exploring solutions to technical problems, and assisting with implementation of individual components.
- **Debugging and troubleshooting:** analyzing error messages, investigating potential causes of issues, and suggesting possible fixes.
- **Testing:** proposing test scenarios, identifying edge cases, and assisting with the development of test cases.
- **Documentation:** drafting and improving technical documentation, setup instructions, project descriptions, and development guidelines.
- **Project planning:** organizing work into tasks, refining acceptance criteria, and structuring development activities.

## Human Review and Responsibility

AI-generated suggestions and outputs were treated as assistance rather than authoritative sources. The team remained responsible for reviewing proposed solutions, adapting them to the project requirements, and verifying their correctness.

Code and documentation produced with AI assistance were expected to be reviewed for correctness, consistency, maintainability, and compatibility with the project architecture. Testing and validation were performed as appropriate to the change.

AI assistance did not replace the team's responsibility for technical decisions, implementation quality, or compliance with project requirements.

## Transparency

This document records the general use of AI tools during the project. It does not imply that every tool was used for every task or that all generated suggestions were incorporated into the final product.

The final implementation and project decisions remain the responsibility of the project team.
