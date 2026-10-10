# Gap Analysis

## Scope

This analysis focuses on the gap around **easy plugin development for LLM gateways**.

The alternatives considered are:

* ALT-01 LiteLLM
* ALT-02 Portkey
* ALT-03 Kong AI Gateway
* ALT-04 Cloudflare AI Gateway

The primary evidence comes from the comparison of these alternatives, especially the properties **Plugin extensibility**, **Plugin implementation languages**, **Plugin scope**, **Plugin execution in the request path**, **Plugin development ergonomics**, **Plugin ecosystem**, **Extensibility without modifying the gateway**, **Deployment and operation**, and **Onboarding**.

The current evidence is strongest for the existence and shape of extension mechanisms. It is weaker for measured developer experience such as time-to-plugin, lines of code, testing workflow, packaging, and deployment. These remain research questions rather than established facts.

---

## GAP-01: A simple, first-class way to build custom LLM Gateway plugins

### Need

**User:** A small development team building or operating an LLM-enabled application.

**Job:** Add company-specific request or response behavior to an LLM gateway without modifying the gateway itself and without having to adopt a large general-purpose API gateway.

Examples of this job include adding a custom check, transformation, guardrail, authentication rule, or provider-specific behavior.

Today, the alternatives expose different extension mechanisms, but none of the current evidence establishes a simple, LLM-focused workflow that makes creating a custom plugin a small and predictable development task.

### Alternatives do not serve it well

The relevant property is **Plugin extensibility**.

* **ALT-01 LiteLLM:** supports Python callbacks, custom guardrails, custom auth, and custom providers. This establishes extensibility, but the current evidence does not establish a deliberately simple plugin-development workflow.
* **ALT-02 Portkey:** supports provider and guardrail plugins and webhooks for custom guardrails, but the observed mechanism is narrower than a general plugin system.
* **ALT-03 Kong AI Gateway:** has a large plugin ecosystem and supports custom plugins in Lua, Go, JavaScript, and Python. This is a strong extension mechanism, but it belongs to a broader API gateway model and the current evidence does not establish a lightweight LLM-specific development workflow.
* **ALT-04 Cloudflare AI Gateway:** does not expose a gateway plugin mechanism in the observed material. Custom logic is implemented through Workers before or after the gateway.

**Evidence properties:**

* `ALT-01` / `Plugin extensibility`
* `ALT-02` / `Plugin extensibility`
* `ALT-03` / `Plugin extensibility`
* `ALT-04` / `Plugin extensibility`
* `ALT-01`–`ALT-04` / `Plugin development ergonomics`

The common problem is not that every alternative lacks extensibility. The problem is that the alternatives expose **different and potentially complex extension models**, and the current comparison does not identify a simple, common workflow for creating LLM Gateway plugins.

### Reachable

A product that closes this gap could provide:

> **A small, first-class plugin API for implementing custom request/response logic in an LLM Gateway, with a minimal development and configuration workflow.**

This stays within the existing LLM Gateway category. It does not require inventing a new product category.

A plugin could be a small piece of code that receives a gateway request, optionally inspects or transforms it, and returns control to the gateway.

### Buildable by a team of 3–4

**Yes.**

A course-sized implementation can focus on a deliberately small plugin API rather than attempting to reproduce the complete extension capabilities of Kong or LiteLLM.

A feasible scope is:

* define a plugin interface;
* expose a small request/response context;
* load plugins from configuration;
* execute plugins at defined gateway lifecycle points;
* allow plugins to modify requests or responses;
* provide one or two example plugins;
* provide a local development workflow.

This does not require implementing a complete enterprise gateway or a large plugin ecosystem.

### Evidence strength

**Medium.**

The existence of different extension mechanisms is directly supported by the alternative comparison. The stronger claim that existing solutions are difficult to extend is **not yet experimentally demonstrated**.

The next research step should therefore measure plugin development effort directly, for example by implementing the same small plugin task against the alternatives where possible.

---

## GAP-02: A plugin development workflow that is easy to learn, test, and deploy

### Need

**User:** A developer who needs to create and maintain gateway plugins as part of an application team.

**Job:** Go from "I need this custom gateway behavior" to a tested and running plugin without having to understand a large gateway architecture or build a separate infrastructure component.

The important need is therefore broader than having a plugin API. The developer needs a usable **plugin development workflow**.

### Alternatives do not serve it well

The relevant properties are **Plugin development ergonomics**, **Deployment and operation**, and **Onboarding**.

The current comparison shows different trade-offs:

* **ALT-01 LiteLLM:** can be started with `pip install` or Docker, and exposes Python callbacks and custom components. However, production operation involves PostgreSQL and typically Redis for multiple instances.
* **ALT-02 Portkey:** has a small TypeScript gateway, but the self-hosted gateway and hosted platform provide different capabilities.
* **ALT-03 Kong AI Gateway:** supports a mature plugin model and multiple plugin languages, but onboarding is steeper for teams unfamiliar with Kong because developers need to work with services, routes, consumers and plugins.
* **ALT-04 Cloudflare AI Gateway:** has very fast gateway onboarding, but custom logic is moved outside the gateway into Workers rather than being developed as gateway plugins.

The alternatives therefore expose a trade-off between **gateway simplicity, plugin extensibility, and operational simplicity**.

**Evidence properties:**

* `ALT-01`–`ALT-04` / `Plugin development ergonomics`
* `ALT-01`–`ALT-04` / `Deployment and operation`
* `ALT-01`–`ALT-04` / `Onboarding`

### Reachable

A product that closes this gap could provide:

> **A single local workflow for creating, running, testing, and configuring an LLM Gateway plugin, using the same plugin interface in development and deployment.**

This is still an LLM Gateway with a plugin system. It does not require a new category.

The workflow could include a minimal plugin template, local execution, configuration, and a way to deploy the same plugin to the gateway.

### Buildable by a team of 3–4

**Yes, if the scope is deliberately constrained.**

The team does not need to build a general-purpose plugin marketplace, multi-language runtime, or enterprise deployment system.

A feasible course implementation could include:

* one plugin language;
* a plugin template or generator;
* local gateway + plugin development mode;
* a simple test harness;
* plugin configuration;
* plugin loading at startup;
* basic error handling.

### Evidence strength

**Medium to low.**

The underlying trade-offs are visible in the comparison, but the claim that developers actually experience these workflows as difficult has not yet been measured.

This gap should therefore be validated with a concrete development task rather than treated as proven solely from feature documentation.

---

## Rejected gaps

The following potential gaps were considered but are **not currently pursued**. They remain recorded because they could be revisited if further evidence changes the decision.

### REJECTED-01: Hard multi-tenant isolation in self-hosted deployments

#### Why it looks like a gap

The comparison shows limitations in configuration isolation:

* ALT-01 provides logical isolation through teams, keys and budgets.
* ALT-02 has stronger isolation in the hosted platform but limited isolation in the self-hosted gateway.
* ALT-03 provides fine-grained isolation, but workspaces and RBAC are enterprise or Konnect features.
* ALT-04 provides isolation through separate gateways but is hosted.

The original comparison therefore identifies configuration isolation as a common weakness.

#### Why we reject it

This is a real limitation, but it is **not sufficiently connected to the primary research question of easy plugin creation**.

It would also expand the product into a substantially broader multi-tenant gateway problem involving authentication, authorization, resource isolation, configuration management, and potentially security boundaries.

A team of 3–4 could build a limited version, but doing so would compete directly with the plugin-development objective for the available course scope.

**Decision: Rejected for the current product direction.**

---

### REJECTED-02: Fully self-hosted governance and observability without state stores

#### Why it looks like a gap

The comparison identifies a trade-off between self-hosting, governance, and operational complexity.

ALT-01 and ALT-03 require additional infrastructure or control-plane components for some capabilities, while the lightweight self-hosted form of ALT-02 provides fewer dashboards and governance capabilities.

#### Why we reject it

This is primarily an **operations and observability** problem rather than a plugin-development problem.

Solving it well would require substantial work around metrics, logs, persistent state, dashboards, aggregation, and possibly distributed operation.

It is also not clear from the current evidence that users would choose a new LLM Gateway primarily to solve this problem.

**Decision: Rejected for the current product direction.**

---

### REJECTED-03: A larger plugin ecosystem or plugin marketplace

#### Why it looks like a gap

Kong has an established plugin ecosystem, while the other alternatives do not show the same ecosystem in the available evidence.

A new gateway could potentially provide a simpler way to share and reuse plugins.

#### Why we reject it

A marketplace is not necessary to validate the core need.

The more fundamental problem is whether an individual developer can **create and use a custom plugin easily**. Building a marketplace would add distribution, discovery, versioning, security review, compatibility, and hosting problems before the core plugin-development experience has been validated.

**Decision: Rejected for the current course scope.**

---

### REJECTED-04: Support for multiple plugin programming languages

#### Why it looks like a gap

Kong supports custom plugins in Lua, Go, JavaScript, and Python, while the observed LiteLLM extension mechanisms are primarily Python-based.

This could suggest a need for developers to use their preferred programming language.

#### Why we reject it

Multiple languages increase implementation complexity substantially: runtime management, dependency isolation, packaging, APIs, debugging, and deployment all become harder.

The current evidence also does not establish that language choice is the user's primary problem.

A simpler single-language plugin API is sufficient to test the core hypothesis.

**Decision: Rejected for the current course scope.**

---

### REJECTED-05: Full enterprise-grade plugin security and sandboxing

#### Why it looks like a gap

Plugins execute gateway logic and can potentially inspect or modify LLM requests. A production plugin system would therefore eventually need strong isolation, permissions, resource limits, and failure handling.

#### Why we reject it

This is an important production concern, but it is too broad for the current course scope.

A fully secure multi-tenant plugin runtime would require significant security engineering and could become the main product rather than supporting the research question.

For the course prototype, the plugin runtime can be constrained to trusted plugins running in the same application environment.

**Decision: Rejected as a primary gap, but retained as a future product requirement.**

---

## Summary of pursued gaps

| ID         | Gap                                                                   | Evidence strength | Course feasibility |
| :--------- | :-------------------------------------------------------------------- | :---------------- | :----------------- |
| **GAP-01** | A simple, first-class way to build custom LLM Gateway plugins         | Medium            | Yes                |
| **GAP-02** | A plugin development workflow that is easy to learn, test, and deploy | Medium–Low        | Yes                |

The two gaps are intentionally closely related.

**GAP-01** concerns the **extension model itself**: how a developer expresses custom gateway behavior.

**GAP-02** concerns the **developer workflow around that model**: how a developer creates, tests, configures, and deploys the plugin.

Together they define a focused product direction without claiming that the existing alternatives have no extensibility.

---

## Open validation questions

Before treating these gaps as fully validated, the following questions should be tested:

1. Can a developer implement the same small gateway plugin in LiteLLM, Portkey, Kong, and the proposed system?
2. How many lines of code are required?
3. How many concepts must the developer learn before the first plugin works?
4. How long does it take to get from a blank project to a working plugin?
5. How easy is it to test a plugin independently?
6. How is plugin configuration supplied?
7. How is a plugin loaded and deployed?
8. What happens when a plugin fails?
9. Can a plugin inspect and modify both requests and responses?
10. Which of these capabilities are actually important to the target user?

These measurements should be used to strengthen or reject `GAP-01` and `GAP-02` rather than assuming that the current feature comparison proves the developer-experience claims.
