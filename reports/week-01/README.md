# Week 01 report

## Project

Modular LLM Gateway, Team 1 — Absolute cinema.

Our problem-space sentence: Companies need a way to let employees use LLMs while applying organization-specific policies for data protection, routing, and request processing.

## What we did

We listed 12 candidates, studied four alternatives, compared their plugin support, and chose two gaps and two value propositions. We held the customer kickoff and set up PRs, Lychee checks, action SHA pins, and Dependabot.

## Findings

LiteLLM, Portkey, Kong, and Cloudflare offer different ways to add custom behavior. Our opportunity is a small plugin API and an easy workflow for creating and testing plugins. The proposed targets of 30 lines per small plugin and 15 minutes to a tested plugin still need validation.

The customer confirmed easy plugin creation as the main goal. Plugins should process requests and responses; routing comes later. A restart to install plugins is acceptable. We plan a first prototype with one provider and a masking plugin. Full decisions and open questions are in the meeting report.

## Coverage

| Deliverable | Artifact |
| --- | --- |
| Candidate list | [candidate-list.md](candidate-list.md) — 12 candidates |
| Alternatives search | [alternatives.md](../../docs/research/alternatives.md); detailed entries: [ALT-01](../../docs/research/alternatives/litellm.md), [ALT-02](../../docs/research/alternatives/portkey.md), [ALT-03](../../docs/research/alternatives/kong-ai-gateway.md), [ALT-04](../../docs/research/alternatives/cloudflare-ai-gateway.md) |
| Compare the alternatives | [comparison.md](../../docs/research/comparison.md) |
| Gap analysis | [gap-analysis.md](../../docs/research/gap-analysis.md) — two pursued and five rejected gaps |
| Value proposition | [value-proposition.md](../../docs/research/value-proposition.md) — VP-01, VP-02, and assumptions A-01–A-09 |
| Meeting script | [meeting-script.md](meeting-script.md) |
| Customer kickoff | [meeting-report.md](meeting-report.md), [meeting-transcript.md](meeting-transcript.md). Recording: private Moodle submission. Transcript publication permission is not yet confirmed. |
| AI usage | [ai-usage.md](ai-usage.md) |

## Contribution

No standalone issues were found. Discussion work below is reported by the team.

| Member | Work |
| --- | --- |
| @b4lmor | Set up the repository and wrote research and meeting documents: [research commit](https://github.com/itpd-absolute-cinema/llm-module-gateway/commit/5b0e0c3449e7dd2dbecac31f66dd37679ad8f5f1). [PR #1](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/1), [#2](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/2), [#3](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/3), [#4](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/4); [approved PR #5](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/5#pullrequestreview-5393205193). |
| @egozhuk | Added Dependabot, [fixed a candidate link](https://github.com/itpd-absolute-cinema/llm-module-gateway/commit/c41943557cbc195a3d7cd81519c312e520486934), and prepared this report. [PR #5](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/5); [reviewed PR #4](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/4#pullrequestreview-5394085613) (review later dismissed). Report PR not yet opened. |
| @m1staken | Reviewed the alternatives analysis and approved it for merging. No authored commit found. [Approved PR #4](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/4#pullrequestreview-5394866728); no authored PR found. |
| @FallenChromium | Took part in oral discussions about future implementation, helped clarify the development approach, and helped plan the team's next steps. Discussion contribution reported by the team; no commit, PR, or GitHub review linked. |

## Repository evidence

[Branch-protection screenshot](images/branch-protection.png),
[merged PR #4](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/4)
with [approval by @m1staken](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/4#pullrequestreview-5394866728),
and [successful Lychee run on main](https://github.com/itpd-absolute-cinema/llm-module-gateway/actions/runs/37042222641).

No URLs are excluded from the link check.

## Deviations

- Get feedback on VP-01 and VP-02; they were not presented at kickoff.
- Oral discussions are listed as contributions without GitHub evidence. No authored PR was found for @m1staken or linked for @FallenChromium.

## Privacy

Privacy confirmation is pending until transcript publication permission and repository history are checked.
