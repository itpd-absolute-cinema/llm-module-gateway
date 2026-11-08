# Features

What the LLM Gateway can do, one capability per entry. Terms such as plugin, request filter and response filter are defined in the [glossary](glossary.md).

## Chat through one endpoint

Send a chat request in the OpenAI format to one endpoint and get a standard completion back, whichever language model answers.

**Stories:** [US-09](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/45), [US-13](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/64)

## Filter requests before the model

Have every chat request validated, masked or sent to another model by your [request filters](glossary.md#request-filter) before it reaches the language model.

**Stories:** [US-10](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/61)

## Filter responses after the model

Have every answer cleaned by your [response filters](glossary.md#response-filter), for example to remove 18+ content, before the client sees it.

**Stories:** [US-11](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/62)

## Learn why a request was refused

See the reason when a [request filter](glossary.md#request-filter) refuses your request, instead of a generic error.

**Stories:** [US-12](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/63), [US-15](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/67)

## Add a plugin

Add a Python [plugin](glossary.md#plugin), choosing whether it filters requests or responses, and have it checked before it is stored.

**Stories:** [US-01](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/17), [US-02](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/18)

## See your plugins

List your [plugins](glossary.md#plugin) and see the type, state and settings of each.

**Stories:** [US-03](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/19)

## Configure a plugin

Change the settings of a [plugin](glossary.md#plugin) without adding it again.

**Stories:** [US-04](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/20)

## Switch a plugin on or off

Turn a [plugin](glossary.md#plugin) on or off at any time to roll out or withdraw a rule, and see the change on the next request.

**Stories:** [US-05](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/22), [US-14](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/65)

## Order your plugins

Decide the order in which your [plugins](glossary.md#plugin) run by placing each one in the [chain](glossary.md#chain).

**Stories:** [US-06](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/23)

## Remove a plugin

Remove a [plugin](glossary.md#plugin) you no longer need.

**Stories:** [US-07](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/24)

## Protect administration with a key

Keep plugin administration behind a secret key, so that only people who hold it can change how the gateway behaves.

**Stories:** [US-17](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/68)

## See how the product is used

A product analyst can see how often the plugin chain completes cleanly and where it fails, using the [events](glossary.md#event) the Gateway and Plugin Manager emit.

**Stories:** US-21
**Behavior:** [FR-007](functional-requirements/fr/FR-007.md)
**Decisions:** [ADR-004](architecture/adr/ADR-004.md)

## Diagnose a quality problem from the operating signals

A system operator can diagnose a failing request from the [observability backend](glossary.md#observability-backend) by following the request identifier across the SLI dashboard and the per-request log line.

**Stories:** US-20
**Behavior:** [FR-008](functional-requirements/fr/FR-008.md)
**Decisions:** [ADR-005](architecture/adr/ADR-005.md)
