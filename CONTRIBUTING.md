# Contributing

This guide describes how to contribute to LLM Gateway. The project is developed as part of the ITPD course.

## Workflow

Every change after the repository's initial commit must go through a pull request. This includes code, documentation, configuration, and CI changes. Do not push changes directly to `main`.

### Branch naming

Create a branch for the task issue you are working on. Use the following format:

`<issue-number>-<short-description>`

Use a short, lowercase description with words separated by hyphens.

Example: `12-add-plugin-error-handling`

### Commit messages

Use Conventional Commits:

`<type>(<scope>): <short description>`

Common types:

- `feat`: a new feature
- `fix`: a bug fix
- `docs`: documentation changes
- `test`: adding or updating tests
- `refactor`: restructuring without changing intended behavior
- `build`: build or dependency changes
- `ci`: continuous integration changes

Examples:

- `feat(gateway): add request routing`
- `fix(interpreter): handle plugin exceptions`
- `docs(workflow): define pull request rules`
- `test(cli): cover invalid arguments`

Keep the description short and specific. Use the scope to identify the affected component when useful.

### Pull requests

A pull request should contain one coherent change and close exactly one task issue.

The pull request description must explain:

- **What changed and why.**
- **What was checked and how.**
- **What the reviewer should inspect**, including whether the linked requirements and task acceptance criteria are satisfied.
- The task issue being closed, linked using `Closes #<issue-number>`.

Before requesting review:

- Check that the task's acceptance criteria are satisfied.
- Run the relevant tests and checks.
- Confirm that the Markdown and link checks pass.
- Update `CHANGELOG.md` under `[Unreleased]` when the change is user-visible.
- Make sure no secrets or private-only material are included.

A team member other than the author must review and approve the pull request before it is merged. The reviewer checks the task's acceptance criteria before approving. Do not merge a pull request without the required approval and passing checks.

The authoritative criteria for completion are in [Definition of Done](docs/definition-of-done.md).

## GitHub Project

The team tracks work in the [LLM Gateway GitHub Project](https://github.com/orgs/itpd-absolute-cinema/projects/1).

The team uses the **Sprint Backlog** table view to see open issues grouped or sorted by `Milestone`. The view includes `Responsible`, `Accountable`, `Assignees`, and `Milestone`, allowing the team to see who is doing the work, who verifies it, and which sprint the issue belongs to.

The GitHub issue and its sprint milestone are the source of truth for scheduling. The project view presents that information and must include every open issue in the repository.

## Definition of Done

A story, task, and pull request are considered done according to the criteria in [`docs/definition-of-done.md`](docs/definition-of-done.md). Keep that document up to date when the team's workflow changes.
