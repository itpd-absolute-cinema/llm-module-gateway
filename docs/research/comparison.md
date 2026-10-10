# Comparison of LLM Gateway alternatives: plugin development

## How to read this document

The primary question of this comparison is:

> **How easily can a team extend an LLM Gateway with its own plugins without modifying or forking the gateway itself?**

This matters when gateway behavior must be adapted to company-specific requirements, such as custom checks, transformations, authentication, guardrails, routing or provider integrations.

The comparison therefore treats **plugin extensibility as the primary dimension**. Data control, deployment, provider integrations, isolation and observability are included as supporting dimensions because they affect the practical value of a plugin architecture.

"Not observed" means the ALT entry does not cover the capability; it does not mean the product lacks it.

Sources: ALT-01 LiteLLM · ALT-02 Portkey · ALT-03 Kong AI Gateway · ALT-04 Cloudflare AI Gateway

## Table

| Property: question, and when it matters                                                                                         | ALT-01 LiteLLM                                                                                                                                                                                             | ALT-02 Portkey                                                                                                                                                              | ALT-03 Kong AI Gateway                                                                                                                              | ALT-04 Cloudflare AI Gateway                                                                                                                                                   |
| :------------------------------------------------------------------------------------------------------------------------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Plugin extensibility.** Can the team add its own behavior without forking the gateway?                                        | **Yes.** MIT core; Python callbacks, custom guardrails, custom auth and custom providers.                                                                                                                  | **Partial.** Provider and guardrail plugins in the open-source gateway; custom guardrails through webhooks. The mechanism is narrower than a general plugin system.         | **Yes.** Large plugin ecosystem; custom plugins can be written in Lua, Go, JavaScript or Python and run in the request path.                        | **No, inside the gateway.** No plugin mechanism is described. Custom logic is implemented through Workers placed before or after the gateway, or through metadata and headers. |
| **Plugin implementation languages.** Can developers use a familiar general-purpose language?                                    | **Python.** Custom callbacks, guardrails, auth and providers are described in Python.                                                                                                                      | **Not fully specified.** The gateway itself is TypeScript; provider and guardrail plugins are supported, but the source does not establish a general plugin-language model. | **Yes.** Lua, Go, JavaScript and Python are explicitly supported for custom plugins.                                                                | **Workers.** Custom logic is moved outside the gateway rather than implemented as gateway plugins.                                                                             |
| **Plugin scope.** Can extensions implement different kinds of gateway behavior?                                                 | **Broad, based on the observed mechanisms:** callbacks, guardrails, authentication and providers.                                                                                                          | **More limited:** provider and guardrail plugins, plus webhooks for custom guardrails.                                                                                      | **Broad:** custom plugins run in the request path and can participate in gateway processing.                                                        | **Externalized:** custom behavior is implemented in Workers rather than as gateway plugins.                                                                                    |
| **Plugin execution in the request path.** Can custom logic directly participate in request processing?                          | **Yes, for the observed callback mechanisms.**                                                                                                                                                             | **Not observed.**                                                                                                                                                           | **Yes.** Custom plugins run in the request path.                                                                                                    | **Not as gateway plugins.** Workers can be placed before or after the gateway.                                                                                                 |
| **Plugin development ergonomics.** Is there evidence that creating a plugin is deliberately optimized for low developer effort? | **Partially observed.** The existence of Python callbacks and custom components suggests an extension mechanism, but the source does not measure development effort or provide a plugin-creation workflow. | **Partially observed.** Plugins and webhooks exist, but the source does not evaluate how easy they are to create.                                                           | **Partially observed.** Multiple plugin languages and an established plugin ecosystem exist, but the source does not measure implementation effort. | **Not observed as a plugin model.** The extension mechanism is Workers rather than gateway plugins.                                                                            |
| **Plugin ecosystem.** Can teams reuse existing extensions?                                                                      | **Not observed.**                                                                                                                                                                                          | **Not observed beyond provider/guardrail plugins.**                                                                                                                         | **Yes.** The source explicitly describes a large plugin ecosystem.                                                                                  | **Not applicable as a gateway-plugin mechanism.**                                                                                                                              |
| **Extensibility without modifying the gateway.** Can company-specific behavior be added independently?                          | **Yes.** Custom callbacks, guardrails, auth and providers are available.                                                                                                                                   | **Yes, within the observed plugin/webhook mechanisms.**                                                                                                                     | **Yes.** Custom plugins are a first-class mechanism.                                                                                                | **Yes, but outside the gateway.** Workers provide the extension point.                                                                                                         |
| **Data control.** Can prompts and logs stay entirely inside the team's own infrastructure?                                      | **Yes.** Self-hosted; keys and spend logs can remain in the team's PostgreSQL.                                                                                                                             | **Partial.** The open-source gateway can be self-hosted, while the hosted platform passes requests and logs through Portkey.                                                | **Yes, when self-managed.** Hybrid mode keeps prompts in the organization's infrastructure.                                                         | **No.** All traffic passes through Cloudflare.                                                                                                                                 |
| **Policy enforcement.** Can plugins or gateway mechanisms implement caller-specific limits and rules?                           | **Yes.** Per-key and per-team budgets, TPM/RPM limits, model allow-lists and guardrail integrations.                                                                                                       | **Yes, mostly hosted.** Routing, retries, caching, guardrails, budgets and rate limits are configurable.                                                                    | **Yes.** Authentication, ACLs, rate limiting, prompt guard and token-based rate limiting.                                                           | **Partial.** Rate limiting, caching, retries, fallbacks, authentication and guardrails/DLP are available, but per-user virtual keys with budgets are not observed.             |
| **Deployment and operation.** What infrastructure must the team operate?                                                        | Docker and Helm; PostgreSQL and typically Redis for several instances.                                                                                                                                     | Small TypeScript gateway; self-hosting provides the proxy while dashboards depend on the platform.                                                                          | Nginx/OpenResty-based gateway; database or DB-less mode and control/data planes make it heavier.                                                    | Nothing to deploy; runs on Cloudflare's network.                                                                                                                               |
| **Provider integrations.** Can plugins or the gateway add custom providers?                                                     | **Yes.** Custom providers are explicitly supported; 100+ providers are available behind an OpenAI-style API.                                                                                               | **Yes, within the provider plugin model;** 200+ LLMs are described.                                                                                                         | **Yes.** Major hosted and self-hosted providers are supported; the list is shorter than LiteLLM and Portkey.                                        | Major hosted providers; arbitrary self-hosted backends are not observed.                                                                                                       |
| **Configuration isolation.** Can plugin configuration and gateway settings be separated between teams?                          | **Logical only.** Teams, keys and budgets separate users, but share one proxy process and model list.                                                                                                      | **Partial.** Workspaces, API keys, virtual keys and versioned configs exist in the platform; self-hosted isolation is limited.                                              | **Yes, at fine grain.** Services, routes, consumers and per-route plugin scoping are available; workspaces/RBAC are enterprise or Konnect.          | **Yes, per gateway.** Separate gateways can have separate settings, logs and authentication.                                                                                   |
| **Observability.** Can the team see the effect and cost of gateway activity?                                                    | Spend and request logs; integrations with Langfuse, OpenTelemetry and Datadog; admin UI per key and team.                                                                                                  | Logs, cost, latency, errors, traces and feedback in the hosted platform.                                                                                                    | Prometheus, OpenTelemetry, Datadog, Splunk and log-shipping plugins; AI plugins emit token usage.                                                   | Built-in dashboard with requests, tokens, cost, errors and cache hits.                                                                                                         |
| **Onboarding.** How quickly can a developer get a working gateway?                                                              | `pip install` or one Docker command, then point an OpenAI client at it.                                                                                                                                    | Change the base URL and add a header or config ID.                                                                                                                          | Easy for teams familiar with Kong; steeper otherwise because of services, routes, consumers and plugins.                                            | Minutes to create a gateway and change the base URL.                                                                                                                           |

## Reading the table as a whole

### The primary question: how extensible is the gateway?

The four alternatives expose fundamentally different extension models.

**LiteLLM** provides an LLM-specific extension mechanism around Python callbacks, custom guardrails, authentication and providers. This makes it possible to add company-specific behavior without forking the core gateway.

**Portkey** provides provider and guardrail plugins and supports custom guardrails through webhooks. The observed extension model is narrower than a general-purpose plugin system.

**Kong** has the most explicit general plugin model in the comparison. Custom plugins can be written in Lua, Go, JavaScript or Python and execute in the request path. It also has a large existing plugin ecosystem.

**Cloudflare AI Gateway** does not expose a gateway plugin mechanism in the observed material. Custom behavior is instead implemented using Workers placed before or after the gateway.

### The important distinction: extensibility versus easy plugin development

The existing evidence establishes that several products are extensible. It does **not** establish that they make plugin development easy.

For this research, these are different questions:

1. **Can I extend the gateway?**
2. **Can I create a plugin without modifying the gateway?**
3. **How much code is required to create the plugin?**
4. **How quickly can a developer create and test one?**
5. **What API does the plugin receive?**
6. **Which parts of the request lifecycle can it access?**
7. **How is plugin configuration supplied?**
8. **How are plugins packaged, versioned and deployed?**
9. **Can plugins be reused across projects?**
10. **Can plugins safely run in a multi-tenant gateway?**

The current alternative research primarily answers questions 1 and 2. Questions 3–10 require additional research.

### The practical trade-off

The alternatives expose three different approaches:

#### LLM-native extension

LiteLLM puts custom behavior relatively close to the LLM gateway itself through Python callbacks and custom components.

#### General API-gateway plugin model

Kong provides a broader plugin architecture where custom plugins are first-class request-path components and can be implemented in several languages.

#### External extension

Cloudflare moves custom behavior outside the gateway into Workers.

Portkey sits between these models, with specific plugin mechanisms for providers and guardrails plus webhooks.

This distinction is more important for this research than a simple "supports plugins / does not support plugins" classification.

## Research gaps

The current comparison is not sufficient to establish which product provides the easiest plugin development experience.

The next research stage should therefore evaluate the following experimentally:

* **Time to first plugin:** how long does it take to implement a minimal plugin?
* **Lines of code:** how much code does the minimal plugin require?
* **API simplicity:** how many concepts must a developer learn?
* **Lifecycle model:** which request/response stages are exposed?
* **Input/output model:** how easily can a plugin inspect and modify requests and responses?
* **Configuration:** how are plugin-specific settings declared and accessed?
* **Dependencies:** how are external libraries handled?
* **Testing:** can plugins be tested independently of the whole gateway?
* **Local development:** is hot reload or an equivalent development workflow available?
* **Packaging:** how is a plugin distributed and versioned?
* **Deployment:** how is a plugin installed into a running gateway?
* **Isolation:** can one plugin or tenant affect another?
* **Failure handling:** what happens when a plugin fails or times out?
* **Documentation:** how complete are the plugin APIs and examples?

These criteria directly measure **plugin developer experience**, rather than merely the existence of an extension mechanism.

## Candidate research hypotheses

1. **Existing LLM gateways provide extensibility, but extensibility does not necessarily imply an easy plugin-development experience.**

2. **LLM-specific gateways and general API gateways expose different plugin abstractions.** LiteLLM is oriented around LLM-specific callbacks and components, while Kong provides a general request-path plugin model.

3. **A gateway can be extensible without having a first-class plugin system.** Cloudflare demonstrates an external extension model through Workers rather than gateway plugins.

4. **The key research opportunity is therefore not simply "does the gateway support plugins?", but "how much effort is required to create, test, configure and deploy a plugin?"**

5. **A useful LLM Gateway plugin architecture should combine the low operational overhead of a lightweight gateway with a first-class extension model that makes custom behavior easy to implement and deploy.**

## Limits of this comparison

The comparison is based on the existing ALT entries. Those entries establish the presence and general form of extension mechanisms, but they do not provide controlled measurements of plugin developer experience.

In particular, the document does not yet provide enough evidence to rank the alternatives by ease of plugin development.

Claims about enterprise features, hosted-only capabilities and supported extension mechanisms can also change with product versions and should be rechecked against the version and date of each underlying ALT entry.
