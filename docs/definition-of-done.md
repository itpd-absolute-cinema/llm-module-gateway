# Definition of Done

## Story

- Every acceptance criterion passes on `main`.
- Every task issue that contributes to the story is closed through a pull request merged into `main`, and each task and pull request meets the requirements below.
- The story issue links evidence for every acceptance criterion.
- The accountable member has checked that every acceptance criterion can be run and verified, and that every link on the story issue resolves.
- The story is closed only after the customer accepts it.

## Task

- Every acceptance criterion attached to the task is satisfied.
- The task is closed as completed only when the pull request that closes it has merged into `main`.

## Pull Request

- The pull request closes exactly one task issue and links that issue using `Closes #<issue-number>`.
- The accountable reviewer checks every task acceptance criterion before approving the pull request.
- Approval comes from a team member other than the pull request author.
- The required repository checks pass, including the Markdown check and link check.
- From Week 4 onward, the product-code CI check also passes.
- If the change is user-visible, the `[Unreleased]` section of `CHANGELOG.md` is updated.
- The acceptance criteria claimed by the pull request pass on its branch and on `main` after merging.
- No secrets, recordings, or private-only material are committed.
- The pull request description records what changed, why it changed, what was checked, how it was checked, and what the reviewer should verify.
- All CI workflows are marked as passed.
