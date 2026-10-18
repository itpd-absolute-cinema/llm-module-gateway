# Meeting Script: Week 08 Customer Sync

## Context

The team has built a prototype LLM Gateway — a Python/FastAPI application that routes requests through a configurable chain of plugins (request and response phases) before and after reaching an LLM provider, with plugins configured via YAML. The customer reviewed the architecture and prototype, raised concerns about plugin failure detection, monitoring, error propagation, and the plugin interface, and asked for a Minimum Usable Product (MVP) with at least three plugins that he can install, run, send prompts through, and extend with his own plugin quickly. What this meeting has to settle: whether the current architecture (plugin runner chain, failure handling, configuration model) is acceptable to build the MVP on, and what exactly the customer will accept as the MVP by end of week.

## Agenda

1. **Permission questions** (5 min) — Ask questions 1–3.
2. **Architecture direction recap and failure/monitoring model** (15 min) — Ask questions 4–6.
3. **Plugin interface and configuration walkthrough** (10 min) — Ask questions 7–8.
4. **MVP scope and acceptance criteria** (10 min) — Ask questions 9–11.
5. **Read-back of decisions and action points** (5 min) — Confirm decisions and owners.

## Questions

1. Is it okay to proceed on the current architecture (FastAPI gateway + ordered plugin chain with request/response phases) as the base for the MVP? — *closed* **(critical)**
2. Do you want the MVP delivered by end of this week as a runnable application with at least three plugins and a documented way to add a new plugin? — *closed* **(critical)**
3. Should the MVP connect to a real LLM provider, or is a mock provider acceptable for this milestone? — *closed*
4. For plugin failure handling, which do you prefer: fail-fast (stop the chain and return an error identifying the failing plugin), graceful degradation (skip non-required plugins and continue), or configurable per-plugin policy? — *open* **(critical)**
5. Should the MVP include per-plugin timing/logging so you can see which plugin ran, in what order, and where it failed? — *closed*
6. When a plugin raises an error, should the error propagate back through previously executed plugins (so they can clean up or transform the error), or be returned directly to the client with a plugin identifier? — *open*
7. Is the current plugin interface (constructor with ID, API version, settings model; `on_request`/`on_response` hooks) acceptable, or do you need a combined plugin that handles both phases with shared state (Django middleware-style)? — *open*
8. Is the YAML-based configuration (provider, plugin list with name/enabled/required, request and response chains, ordering) acceptable for the MVP? — *closed*
9. For the MVP acceptance test, is this sufficient: you clone the repo, follow setup steps, start the app, send a prompt, see it pass through three plugins with visible transformations, and add your own plugin by following documentation? — *closed* **(critical)**
10. Do you require a plugin library (multiple ready-made plugins) shipped with the MVP, or is one example plugin plus documentation enough? — *closed*
11. What is the minimum number of plugins and which behaviors (e.g., PII masking, logging, content filtering) must be present in the MVP for you to consider it usable? — *open*

## Roles

- **Moderator:** Artem L.

## Key improvements

1. **Before:** "How will you find out if one of the plugins doesn't work?" — vague, asks the team to describe, not decide.
   **After:** "For plugin failure handling, which do you prefer: fail-fast, graceful degradation, or configurable per-plugin policy?" — forces a concrete choice that changes what the team builds.
   **Principle:** Replace open-ended "how" questions with closed or constrained-choice questions that produce a decision.

2. **Before:** "Is it for you okay that the configuration of our LLM Gateway is something like this?" — yes/no with no content, answer changes nothing specific.
   **After:** "Is the YAML-based configuration (provider, plugin list with name/enabled/required, request and response chains, ordering) acceptable for the MVP?" — names the exact artifact under review so a "no" points to a concrete change.
   **Principle:** Anchor approval questions to the specific artifact shown, so the answer is actionable.

3. **Before:** "What do you exactly wait from us to next meeting?" — ambiguous, invites a restatement of everything.
   **After:** "For the MVP acceptance test, is this sufficient: clone, set up, start, send a prompt, see three plugins transform it, add your own plugin via docs?" — turns the scope question into a testable acceptance criterion.
   **Principle:** Convert scope questions into acceptance criteria the team can verify against.
