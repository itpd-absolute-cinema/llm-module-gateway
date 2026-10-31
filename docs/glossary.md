# Glossary

## Plugin

A Python file that applies a company's rules to requests, responses, or provider selection without changing the Gateway core.

**Defined by:** [Product vision](product-vision.md#goal) and [architecture decision DEC-009](decisions.md#dec-009-architecture).

## Provider

An external LLM service that generates responses to requests sent by the Gateway.

**Defined by:** [Product boundary BND-01](product-vision.md#bnd-01).

## Chain

The order in which the enabled [plugins](#plugin) of one [type](#plugin-type) run. Request filters run in the chain order before the language model, and response filters run in the chain order after it.

**Defined by:** [Functional CLI threshold](threshold-of-success.md#tos-03-functional-cli).

## Enabled plugin

A [plugin](#plugin) that takes part in chat processing. A plugin that is not enabled is *disabled*: it stays installed with its [settings](#settings) and its place in the [chain](#chain), but it does not run.

## Gateway

The service that applications call for chat completions. It passes each request through the [request filters](#request-filter), asks the language model, and passes the answer through the [response filters](#response-filter).

**Defined by:** [Architecture decision DEC-009](decisions.md#dec-009-architecture).

## Plugin Manager

The internal service that runs [plugins](#plugin) for the [gateway](#gateway). The gateway sends it the data and the [enabled plugins](#enabled-plugin) of the needed [type](#plugin-type) in [chain](#chain) order.

## Plugin type

Whether a [plugin](#plugin) is a [request filter](#request-filter) or a [response filter](#response-filter). The administrator chooses it when adding the plugin.

## Plugins folder

The folder that holds the files of all [plugins](#plugin). The [gateway](#gateway) and the [Plugin Manager](#plugin-manager) share it.

## Request filter

A [plugin](#plugin) that processes a chat request before it reaches the language model. It can validate the data, mask personal data, send the request to another model, or refuse the request.

## Response filter

A [plugin](#plugin) that processes the language model's answer before the client sees it, for example to remove 18+ content.

## Settings

The values an administrator gives a [plugin](#plugin) to tune its behaviour, such as the text that replaces masked data. The [gateway](#gateway) passes them to the plugin on every call. The API calls them *options*.
