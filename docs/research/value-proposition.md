# Value Proposition

Both propositions target the same product direction: an LLM Gateway where writing a custom plugin is a small, predictable task. They are built on the two gaps we pursue and respect the scope decisions recorded in the rejected gaps (no marketplace, no multi-language runtime, no sandboxing, no multi-tenant isolation).

---

## VP-01

A custom gateway behavior in one small file

- **Status:** Active
- **User:** developer on a small team that runs an LLM-enabled application and needs company-specific request/response logic (a check, a transformation, a guardrail, an auth rule).
- **Problem:** the alternatives are extensible, but each through a different and potentially heavy model: LiteLLM callbacks and custom components, Kong's service/route/consumer/plugin model, Portkey's narrower guardrail and webhook mechanism, Cloudflare's external Workers.
  None of them is documented as a deliberately small plugin task, so the effort of a first plugin is unknown and probably not small.
- **What we do that the alternatives do not:** one LLM-specific plugin interface: a plugin is a single class/function that receives a small request/response context, may inspect or modify it, and returns control to the gateway.
  Plugins are loaded from configuration and run at a few fixed lifecycle points.
  The interface is designed around a measurable budget: a minimal plugin (for example, rejecting requests that contain a banned pattern, or adding a header) takes a provisional target of at most 30 lines of plugin code and at most 3 concepts to learn (see [ASM-02](../assumptions.md#asm-02)).
- **Closes:** [GAP-01](gap-analysis.md#gap-01).
- **Rests on:** [ASM-01](../assumptions.md#asm-01), [ASM-02](../assumptions.md#asm-02), [ASM-04](../assumptions.md#asm-04), [ASM-05](../assumptions.md#asm-05), [ASM-06](../assumptions.md#asm-06), [ASM-07](../assumptions.md#asm-07), [ASM-08](../assumptions.md#asm-08), [ASM-09](../assumptions.md#asm-09).
- **What it costs:**
  - a deliberately small API: fewer lifecycle points and less plugin scope than Kong or LiteLLM, so some advanced use cases are not possible;
  - one plugin language, so teams outside that language cannot use it;
  - trusted plugins only, with no sandboxing or resource isolation (REJECTED-05);
  - no plugin ecosystem to reuse (REJECTED-03). Kong has one, we do not.

  These are real limits, and they are the price of the simplicity.
- **How a competitor would respond:** LiteLLM already has Python callbacks and could document a minimal plugin interface and template in a release.
  Kong could ship an LLM-focused plugin template on top of its existing plugin development kit.
  Copying the *interface* would take them weeks, so the interface is not a moat.
  The defensible part is the discipline of treating plugin effort (lines of code, concepts, time to first plugin) as the primary product constraint and publishing the benchmark.
  A general gateway has other priorities and cannot trade away its breadth for this.

---

## VP-02

From blank project to a tested, running plugin in one local workflow

- **Status:** Active
- **User:** developer on an application team who has to create and maintain gateway plugins, but is not a gateway specialist.
- **Problem:** the path from "I need this behavior" to a tested, deployed plugin goes through gateway architecture and extra infrastructure.
  LiteLLM in production adds PostgreSQL and typically Redis.
  Kong needs services, routes, consumers and a heavier control/data plane.
  Portkey's self-hosted gateway and hosted platform differ in capabilities.
  Cloudflare has the fastest onboarding but moves custom logic outside the gateway.
- **What we do that the alternatives do not:** one local workflow with a single plugin interface for development and deployment: a command generates a plugin from a template, the gateway runs locally with the plugin loaded, a test harness runs the plugin against sample requests without starting the whole gateway, and the same plugin is enabled in a deployed gateway through configuration only.
  Provisional targets: a working, tested plugin from a blank project in at most 15 minutes, and a unit test that runs without a live gateway or LLM provider (see [ASM-03](../assumptions.md#asm-03)).
- **Closes:** [GAP-02](gap-analysis.md#gap-02).
- **Rests on:** [ASM-01](../assumptions.md#asm-01), [ASM-03](../assumptions.md#asm-03), [ASM-05](../assumptions.md#asm-05), [ASM-07](../assumptions.md#asm-07), [ASM-08](../assumptions.md#asm-08), [ASM-09](../assumptions.md#asm-09).
- **Changed:**
  - The kickoff scoped the first runnable example to a proxy, one provider, and one masking plugin ([DEC-004](../decisions.md)); coding-agent instructions remain a nice-to-have ([DEC-005](../decisions.md)).
- **What it costs:**
  - the gateway itself is narrower: fewer providers, fewer built-in governance and observability features than LiteLLM, Portkey or Kong (REJECTED-01, REJECTED-02);
  - the workflow supports one language and one deployment style, so it will not fit every team's environment;
  - we maintain the template, the harness and the local mode, which is work that does not add gateway features.
- **How a competitor would respond:** LiteLLM could add a pytest helper, a cookiecutter template and a "local dev" doc page in a few weeks, which would close much of this gap.
  Cloudflare could add a local test runner for Workers around the gateway.
  Of the two propositions this is the easier one to copy.
  The defensible part is only that the interface and the workflow are designed together, so the harness never has to emulate a large gateway.
  We should not present this as a moat.

## Assumptions

The comparison and the gap analysis establish that extension mechanisms exist and differ. They do not measure developer effort. Every claim above that depends on effort is therefore an assumption until tested.

The Week 1 entries `A-01`–`A-09` now live in the [assumptions log](../assumptions.md) as `ASM-01`–`ASM-09`, in the same order.
The `Rests on` fields above preserve the original table's dependencies.
