# Agent Instructions

For course rules, start with [SKILL.md](https://github.com/inno-itpd/itpd/blob/main/SKILL.md).

## Repository Layout

- `docs/`: project documentation. Put new project documents here.
- `docs/architecture/`: diagrams and their source files.
- `docs/research/`: alternatives, comparisons, gaps, and the value proposition.
- `compose.yml`: the container setup for local use and VM deployments.
- `reports/week-NN/`: weekly reports, meeting scripts, and transcripts.
- `.github/ISSUE_TEMPLATE/`: issue forms.
- `.github/workflows/`: CI checks and documentation deployment.
- `mkdocs.yml`: documentation navigation. Add new pages to this menu.

Application code is under `code/`, and the Python prototype is under `prototype/`.

## Checks

Run checks from the repository root:

Check Markdown with `npx --yes markdownlint-cli2@0.23.2 '**/*.md'`.
The same check runs in `.github/workflows/markdown.yml`.

Check links with `lychee --no-progress --max-concurrency 2 --accept 200,206,429 './**/*.md'`.
This needs network access and runs in `.github/workflows/lychee.yml`.

To build the documentation, install dependencies with `python -m pip install --require-hashes -r requirements-docs.txt`, then run `python -m mkdocs build --strict`.
The build also runs in `.github/workflows/docs.yml`.

## Conventions

- Follow `CONTRIBUTING.md`. Name task branches `<issue-number>-<short-description>` and use Conventional Commits: `<type>(<scope>): <short description>`.
- Open a pull request for every change. Merge only after another team member approves and checks pass. Do not push directly to `main`.
- Write documentation in English. Keep existing identifiers and anchors, and follow `.markdownlint-cli2.jsonc` and the style of nearby files.
- Use made-up data in examples. Never commit credentials or private information.
- Update this file when the layout, checks, or conventions change. Update the glossary when a term's meaning changes. Include these updates in the same pull request as the change.

## Files Not to Edit

- `reports/week-01/meeting-report.md`, `reports/week-02/meeting-report.md`, and `reports/week-03/meeting-report.md`: leave past meeting records as they are. Record follow-up work in the current week's report. If an old report has a factual error, a team member corrects it using the original meeting notes or transcript.
- `reports/week-01/meeting-transcript.md`, `reports/week-02/meeting-transcript.vtt`, and `reports/week-03/meeting-transcript.md`: leave recorded statements as they are. A team member makes corrections against the recording or removes private information when needed.
- `LICENSE`: leave license changes to a repository owner after the team agrees on them.
