# Configuration Plan

**Reviewed:** 2026-11-21

## Source control and release

### CFG-01

**Status:** Active
**Type:** Repository
**Purpose:** Holds the product source and its history; without it there is no reviewable change and no release tag.
**Owner:** @b4lmor
**Where:** the GitHub repository `itpd-absolute-cinema/llm-module-gateway`
**Connections:** Release tags point at commits in this repository; a force-push breaks every tag.
**What good looks like:** Every release tag points at a commit on `main` and is protected.

### CFG-02

**Status:** Active
**Type:** Release tag
**Purpose:** Marks the release commit; without it the deployed version cannot be identified.
**Owner:** @b4lmor
**Where:** GitHub Releases, tags `v*`
**Connections:** The changelog section links the tag; moving it breaks the release record.
**What good looks like:** The tag is protected from being moved or deleted.

## Checks

### CFG-03

**Status:** Active
**Type:** CI workflow
**Purpose:** Runs link check, Markdown check, actionlint, and product-code CI on every PR and on `main`; without it a broken link or a failing test reaches `main`.
**Owner:** @egozhuk
**Where:** `.github/workflows/`
**Connections:** The Definition of Done's pull request list names these checks; a red check blocks the merge.
**What good looks like:** Every required check is green on `main` before submission.

## Environments and secrets

### CFG-04

**Status:** Active
**Type:** Environment file
**Purpose:** Holds the staging and production configuration; without it the deployment cannot start.
**Owner:** @FallenChromium
**Where:** `/opt/llm-gateway-staging/.env` and `/opt/llm-gateway-production/.env` on the university VM
**Connections:** `IMAGE_TAG` in this file selects the deployed image; a wrong tag deploys the wrong version.
**What good looks like:** The file follows `.env.example`, secrets are never committed, permissions are operator-only.

### CFG-05

**Status:** Active
**Type:** Secret
**Purpose:** The admin API key that permits plugin upload; without it plugin management is open to anyone who reaches the port.
**Owner:** @FallenChromium
**Where:** the `.env` files, never in the repository
**Connections:** The Gateway admin API reads it at start; a leaked key breaks the admin authorization boundary.
**What good looks like:** A different random key per environment, rotated on leak.

## Dependencies and builds

### CFG-06

**Status:** Active
**Type:** Package manifest
**Purpose:** Pins the Python and Java dependencies; without it a build can pull a different version each time.
**Owner:** @egozhuk
**Where:** the Python and Java package manifests in the repository
**Connections:** The image build reads them; an unpinned dependency breaks a reproducible build.
**What good looks like:** Every dependency is pinned, the lock file is committed.

### CFG-07

**Status:** Active
**Type:** Image workflow
**Purpose:** Builds and publishes the `sha-<short-commit>` image the deployment pulls; without it the VM cannot deploy a specific commit.
**Owner:** @egozhuk
**Where:** `.github/workflows/`
**Connections:** `IMAGE_TAG` in the environment file names the image; a missing image breaks the deploy.
**What good looks like:** The workflow publishes an image for every commit on `main`.

## Agent context and artifacts

### CFG-08

**Status:** Active
**Type:** Agent instruction
**Purpose:** Tells an agent how the repository works; without it an agent guesses and edits the wrong files.
**Owner:** @b4lmor
**Where:** `AGENTS.md` in the repository root
**Connections:** Links the course `SKILL.md`; a stale `AGENTS.md` sends an agent to the wrong layout.
**What good looks like:** The layout, checks, conventions, and off-limits files are current.

### CFG-09

**Status:** Active
**Type:** Documentation index
**Purpose:** The architecture, requirements, features, and glossary indexes; without them a reader cannot find what owns a fact.
**Owner:** @FallenChromium
**Where:** `docs/architecture/README.md`, `docs/functional-requirements/README.md`, `docs/quality-requirements/README.md`, `docs/features.md`, `docs/glossary.md`
**Connections:** The weekly report links them; a missing index breaks the trace from a driver to its record.
**What good looks like:** Each index lists every active item once and links the file.