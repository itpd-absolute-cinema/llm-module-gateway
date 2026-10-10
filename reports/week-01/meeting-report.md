# Kickoff meeting report

## Metadata

**Date:** 2026-10-02
**Duration:** 49 minutes
**Attended:** Artem, Azamat, Customer
**Presented:** our reading of the original description (data filtering), our questions on scope, and a first MVP sketch (proxy, token from `.env`, one provider, one masking plugin). We did not present `VP-01` and `VP-02`.
**Recording:** permitted, Customer started it himself, link in the Moodle submission only
**Transcript publication:** not asked as a separate question, to be confirmed with Customer in writing before the transcript is committed, see [the transcript](meeting-transcript.md)
**Transcript shared privately:** not asked
**Script:** [meeting-script.md](meeting-script.md)

## Summary

- Customer confirmed that the product is the plugin system itself, with easy plugin creation, and not data filtering; filtering is one example plugin. This supports `GAP-01` and `VP-01`.
- Customer ranked what plugins must reach: the user request first, the LLM response second, routing third.
- Plugins are loaded by restarting the gateway, not on the fly. Customer wants no industrial-grade system, but a stated upper limit on the number of plugins running together.
- Customer prefers Python and approved Go, discouraged a new plugin language, and accepted Java for a team that knows it. One or two LLM providers are enough.
- First runnable version (MVP-0) is a small proxy with one masking plugin, planned for about two to three weeks.

## Decisions

| Decision                                                                                                   | Made by                     | Traces to |
| ---------------------------------------------------------------------------------------------------------- | --------------------------- | --------- |
| Plugins are installed by restarting or redeploying the gateway, with no loading on the fly                 | Customer                    | `GAP-02`  |
| Plugins must reach the user request first, the LLM response second and routing third                       | Customer                    | `GAP-01`  |
| Plugins are written in a mainstream language that LLMs already know, not in a new domain-specific language | Customer                    | `GAP-01`  |
| MVP-0 is a proxy with a token from `.env`, one provider and one simple masking plugin                      | Team, not contested         | `VP-02`   |
| A guide or skill for coding agents on writing plugins is a nice-to-have                                    | Team, supported by Customer | `VP-02`   |
| Support one or two LLM providers, with depth over breadth                                                  | Customer                    | None      |
| Start with a simple, configurable login and simple provider-key storage with a security warning            | Customer                    | None      |
| The architecture states an upper limit on plugins running at the same time                                 | Customer                    | None      |

## Action points

| Action                                                                                                        | Owner | Due           |
| ------------------------------------------------------------------------------------------------------------- | ----- | ------------- |
| Choose the gateway and plugin language (Python, Go or Java) and write down the reason                         | artem | End of Week 2 |
| Update `VP-01`, `VP-02` and the assumptions table with restart-only loading, hook priorities and the language | artem | End of Week 2 |
| Set up the repository with CI and linters                                                                     | artem | End of Week 2 |
| Build MVP-0: proxy, `.env` token, one provider, masking plugin                                                | artem | End of Week 3 |
| Write the analysis of the plugin limit that follows from our architecture                                     | artem | End of Week 4 |

## Open questions

| Question                                                                             | What it would change                              | Follow-up                               |
| ------------------------------------------------------------------------------------ | ------------------------------------------------- | --------------------------------------- |
| Which language do we use for the gateway and for plugins?                            | `VP-01` (one plugin language) and assumption A-05 | artem and azamat, Week 2                |
| Should provider keys live in the gateway or in an external token manager?            | Authentication and storage design                 | azamat, carried into the Friday meeting |
| Should users be able to bring their own provider tokens, and as core or as a plugin? | Scope of the authentication plugin                | artem, carried into the Friday meeting  |
| How many plugins should run together, and how do plugins pass data to each other?    | Plugin lifecycle and chaining design              | artem, Week 4                           |

## Disagreements

| Your position                                                                                                | Customer's position                                                                              | What you changed                                                                                       |
| ------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| The original description reads as one use case, filtering personal data                                      | The point is a modular system where plugins cover filtering, logging, access control and routing | We now describe the product as the plugin system, with masking as one example plugin                   |
| A custom domain-specific language could be used for plugins                                                  | An LLM writes a new language badly and needs long documentation                                  | We dropped the idea and plan plugins in a mainstream language                                          |
| The gateway is written in Java, because all teammates know it                                                | Prefers Python, approves Go, would rather avoid Java but accepts it                              | Nothing yet; Java stays an option because the whole team knows it, and the decision is an action point |
| Users should be able to use their own provider tokens, as in a company where employees use personal accounts | A pool of company keys is the proper way, and building this in encourages a bad practice         | Kept out of the core; it would be an optional plugin if time allows                                    |
