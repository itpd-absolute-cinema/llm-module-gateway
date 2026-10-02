# Value Proposition

Both propositions target the same product direction: an LLM Gateway where writing a custom plugin is a small, predictable task. They are built on the two gaps we pursue and respect the scope decisions recorded in the rejected gaps (no marketplace, no multi-language runtime, no sandboxing, no multi-tenant isolation).

---

## VP-01: A custom gateway behavior in one small file

**User:** developer on a small team that runs an LLM-enabled application and needs company-specific request/response logic (a check, a transformation, a guardrail, an auth rule).
**Problem:** the alternatives are extensible, but each through a different and potentially heavy model: LiteLLM callbacks and custom components, Kong's service/route/consumer/plugin model, Portkey's narrower guardrail and webhook mechanism, Cloudflare's external Workers. None of them is documented as a deliberately small plugin task, so the effort of a first plugin is unknown and probably not small.
**What we do that the alternatives do not:** one LLM-specific plugin interface: a plugin is a single class/function that receives a small request/response context, may inspect or modify it, and returns control to the gateway. Plugins are loaded from configuration and run at a few fixed lifecycle points. The interface is designed around a measurable budget: a minimal plugin (for example, rejecting requests that contain a banned pattern, or adding a header) takes a provisional target of at most 30 lines of plugin code and at most 3 concepts to learn (see A-02).
**Closes:** [GAP-01](gap-analysis.md#gap-01-a-simple-first-class-way-to-build-custom-llm-gateway-plugins).
**What it costs:**
* a deliberately small API: fewer lifecycle points and less plugin scope than Kong or LiteLLM, so some advanced use cases are not possible;
* one plugin language, so teams outside that language cannot use it;
* trusted plugins only, with no sandboxing or resource isolation (REJECTED-05);
* no plugin ecosystem to reuse (REJECTED-03). Kong has one, we do not.

These are real limits, and they are the price of the simplicity.
**How a competitor would respond:** LiteLLM already has Python callbacks and could document a minimal plugin interface and template in a release. Kong could ship an LLM-focused plugin template on top of its existing plugin development kit. Copying the *interface* would take them weeks, so the interface is not a moat. The defensible part is the discipline of treating plugin effort (lines of code, concepts, time to first plugin) as the primary product constraint and publishing the benchmark. A general gateway has other priorities and cannot trade away its breadth for this.

---

## VP-02: From blank project to a tested, running plugin in one local workflow

**User:** developer on an application team who has to create and maintain gateway plugins, but is not a gateway specialist.
**Problem:** the path from "I need this behavior" to a tested, deployed plugin goes through gateway architecture and extra infrastructure. LiteLLM in production adds PostgreSQL and typically Redis. Kong needs services, routes, consumers and a heavier control/data plane. Portkey's self-hosted gateway and hosted platform differ in capabilities. Cloudflare has the fastest onboarding but moves custom logic outside the gateway.
**What we do that the alternatives do not:** one local workflow with a single plugin interface for development and deployment: a command generates a plugin from a template, the gateway runs locally with the plugin loaded, a test harness runs the plugin against sample requests without starting the whole gateway, and the same plugin is enabled in a deployed gateway through configuration only. Provisional targets: a working, tested plugin from a blank project in at most 15 minutes, and a unit test that runs without a live gateway or LLM provider (see A-03).
**Closes:** [GAP-02](gap-analysis.md#gap-02-a-plugin-development-workflow-that-is-easy-to-learn-test-and-deploy).
**What it costs:**
* the gateway itself is narrower: fewer providers, fewer built-in governance and observability features than LiteLLM, Portkey or Kong (REJECTED-01, REJECTED-02);
* the workflow supports one language and one deployment style, so it will not fit every team's environment;
* we maintain the template, the harness and the local mode, which is work that does not add gateway features.

**How a competitor would respond:** LiteLLM could add a pytest helper, a cookiecutter template and a "local dev" doc page in a few weeks, which would close much of this gap. Cloudflare could add a local test runner for Workers around the gateway. Of the two propositions this is the easier one to copy. The defensible part is only that the interface and the workflow are designed together, so the harness never has to emulate a large gateway. We should not present this as a moat.

---

# Assumptions

The comparison and the gap analysis establish that extension mechanisms exist and differ. They do not measure developer effort. Every claim above that depends on effort is therefore an assumption until tested.

| ID   | Assumption                                                                                                         | Used by       | Why it matters                                                                            | How to test                                                                                                                                       |
| :--- | :----------------------------------------------------------------------------------------------------------------- | :------------ | :---------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------ |
| A-01 | Small teams have a recurring need for company-specific gateway logic and would choose a gateway because of it.     | VP-01, VP-02  | If the need is rare, ease of plugin development is not a reason to switch gateways.       | Short interviews with 5+ developers who run an LLM application; ask which custom behaviors they added and how they did it (open question 10).  |
| A-02 | Writing the same minimal plugin in LiteLLM and Kong takes materially more code and concepts than our 30-line, 3-concept target. | VP-01 | This is the core claim. If the alternatives are already about as simple, there is no gap. | Implement one identical minimal plugin in LiteLLM, Portkey (where possible), Kong and our prototype. Record lines of code and concepts (open questions 1–3). |
| A-03 | Time from blank project to a working, tested plugin is materially longer in the alternatives than our 15-minute target. | VP-02 | The workflow claim needs a number someone can check.                                      | Time the same task on each alternative with a developer new to it (open questions 4–5, 7). The thresholds in VP-01 and VP-02 are provisional and will be reset after this benchmark. |
| A-04 | Access to the request and response through a small context object is enough for the target plugins.               | VP-01         | If common plugins need more lifecycle points, the small API will not hold.               | List 5–10 realistic plugins (check, redaction, auth rule, header rewrite, provider tweak) and implement each against the prototype API (open question 9). |
| A-05 | One plugin language is acceptable to the target user.                                                              | VP-01, VP-02  | A wrong choice of language excludes users and weakens both propositions.                 | Ask interviewees about their stack (see A-01); choose the language by majority and record the decision.                                           |
| A-06 | Trusted plugins in the gateway's own process are acceptable for the target user.                                   | VP-01         | If users need isolation from the start, REJECTED-05 would have to be reopened.           | Ask in the same interviews who writes plugins and who runs the gateway; if they are the same team, the assumption holds.                          |
| A-07 | A plugin failure can be handled with a simple, predictable rule (for example, fail closed or fail open per plugin) without harming the user's application. | VP-01, VP-02 | Plugins sit in the request path; unpredictable failures would make plugins unsafe to use. | Define the rule, inject failures and timeouts in the prototype, check behavior (open question 8).                                                 |
| A-08 | LiteLLM or Kong would need more than a minor release to match the plugin-effort numbers, and would not reprioritize quickly. | VP-01, VP-02 | Determines whether the advantage lasts beyond the course project.                        | Review their public roadmaps and recent releases for plugin tooling; recheck against the version and date of each ALT entry.                       |
| A-09 | The ALT entries are still accurate (versions, enterprise-only and hosted-only features).                           | VP-01, VP-02  | The comparison notes that such claims change between product versions.                    | Recheck each ALT entry against current documentation before the final submission.                                                                  |
