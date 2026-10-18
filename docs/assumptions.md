# Assumptions

## ASM-01

Small teams have a recurring need for company-specific gateway logic and would choose a gateway because of it.

- **Status:** Open
- **How to check:** Short interviews with 5+ developers who run an LLM application; ask which custom behaviors they added and how they did it ([open question 10](research/gap-analysis.md#open-validation-questions)). Week not specified in the source.
- **Why it matters:** If the need is rare, ease of plugin development is not a reason to switch gateways.

## ASM-02

Writing the same minimal plugin in LiteLLM and Kong takes materially more code and concepts than our 30-line, 3-concept target.

- **Status:** Open
- **How to check:** Implement one identical minimal plugin in LiteLLM, Portkey (where possible), Kong and our prototype. Record lines of code and concepts ([open questions 1–3](research/gap-analysis.md#open-validation-questions)). Week not specified in the source.
- **Why it matters:** This is the core claim. If the alternatives are already about as simple, there is no gap.

## ASM-03

Time from blank project to a working, tested plugin is materially longer in the alternatives than our 15-minute target.

- **Status:** Open
- **How to check:** Time the same task on each alternative with a developer new to it ([open questions 4–5, 7](research/gap-analysis.md#open-validation-questions)). The thresholds in VP-01 and VP-02 are provisional and will be reset after this benchmark. Week not specified in the source.
- **Why it matters:** The workflow claim needs a number someone can check.

## ASM-04

Access to the request and response through a small context object is enough for the target plugins.

- **Status:** Open
- **How to check:** List 5–10 realistic plugins (check, redaction, auth rule, header rewrite, provider tweak) and implement each against the prototype API ([open question 9](research/gap-analysis.md#open-validation-questions)). Week not specified in the source.
- **Why it matters:** If common plugins need more lifecycle points, the small API will not hold.

## ASM-05

One plugin language is acceptable to the target user.

- **Status:** Open
- **How to check:** Ask interviewees about their stack (see [ASM-01](#asm-01)); choose the language by majority and record the decision. Week not specified in the source.
- **Why it matters:** A wrong choice of language excludes users and weakens both propositions.

## ASM-06

Trusted plugins in the gateway's own process are acceptable for the target user.

- **Status:** Open
- **How to check:** Ask in the same interviews who writes plugins and who runs the gateway; if they are the same team, the assumption holds. Week not specified in the source.
- **Why it matters:** If users need isolation from the start, [REJECTED-05](research/gap-analysis.md#rejected-05-full-enterprise-grade-plugin-security-and-sandboxing) would have to be reopened.

## ASM-07

A plugin failure can be handled with a simple, predictable rule (for example, fail closed or fail open per plugin) without harming the user's application.

- **Status:** Open
- **How to check:** Define the rule, inject failures and timeouts in the prototype, check behavior ([open question 8](research/gap-analysis.md#open-validation-questions)). Week not specified in the source.
- **Why it matters:** Plugins sit in the request path; unpredictable failures would make plugins unsafe to use.

## ASM-08

LiteLLM or Kong would need more than a minor release to match the plugin-effort numbers, and would not reprioritize quickly.

- **Status:** Open
- **How to check:** Review their public roadmaps and recent releases for plugin tooling; recheck against the version and date of each ALT entry. Week not specified in the source.
- **Why it matters:** Determines whether the advantage lasts beyond the course project.

## ASM-09

The ALT entries are still accurate (versions, enterprise-only and hosted-only features).

- **Status:** Open
- **How to check:** Recheck each ALT entry against current documentation before the final submission. No specific week was recorded in the source.
- **Why it matters:** The comparison notes that such claims change between product versions.
