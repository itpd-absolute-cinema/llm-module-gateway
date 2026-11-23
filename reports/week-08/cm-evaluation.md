# CM Evaluation

**Baseline:** v0.7.0
**Reviewed:** 2026-11-21

## Method

The team reviewed the configuration on 2026-11-21 against the `v0.7.0` tag, in the order of the five groups in `docs/configuration-plan.md`, reading the repository rather than a memory of it. Out of scope: the university VM's operating system and the customer's own network.

## Goal: Decide what the product should be

**Today:** The product vision and the threshold of success carry the decision ([CFG-09](#cfg-09)).
**A more convenient way:** Keep — the two artifacts are read together and neither duplicates the other.
**Why:** The goal is achieved, and no mechanism would be removed by a change.
**Tradeoffs:** Any merge would lose the split between "what the product is for" and "how we know it succeeded".

## Goal: Specify what the product must do

**Today:** The functional and quality requirement indexes, the ADRs, and the features page ([CFG-09](#cfg-09)).
**A more convenient way:** Merge the features page into the functional requirements index.
**Why:** The goal is still achieved, and the change removes the second list of capabilities that the features page maintains by hand.
**Tradeoffs:** The features page is the only user-language view; merging it loses that view for a reader who is not on the team.

## Goal: Verify the product

**Today:** The CI workflows and the image workflow ([CFG-03](#cfg-03), [CFG-07](#cfg-07)).
**A more convenient way:** Keep — the checks and the image build are separate and both are read on every PR.
**Why:** The goal is achieved, and no mechanism would be removed.
**Tradeoffs:** Any merge would lose the separation between "does it pass" and "can it deploy".

## Goal: Keep the context trustworthy for an agent

**Today:** `AGENTS.md` and the course `SKILL.md` ([CFG-08](#cfg-08)).
**A more convenient way:** Add a one-line pointer from `AGENTS.md` to the configuration plan, so an agent reads the inventory before editing a configuration file.
**Why:** The goal is still achieved, and the change adds a mechanism that is missing today.
**Tradeoffs:** The pointer adds one line to `AGENTS.md`; the cost is that the file must be updated when the plan's path changes.

## What We Could Not Judge

None. Every item in the plan was judged.

## The First Change

The first change is the pointer from `AGENTS.md` to the configuration plan. It keeps `AGENTS.md` as the entry point and adds one line; it removes nothing. Its main tradeoff is that `AGENTS.md` must be updated when the plan's path changes.