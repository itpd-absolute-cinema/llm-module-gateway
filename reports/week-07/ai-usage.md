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
