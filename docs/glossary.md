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

### Focus metric

The single metric that best expresses whether the product delivers its value proposition. Defined in [Analytics Plan](analytics.md#gateway_plugin_chain_completeness_ratio).

### Input metric

A metric the team can directly influence that moves the focus metric. Defined in [Analytics Plan](analytics.md#gateway_plugin_execution_success_total).

### Counter-metric

A metric that guards against gaming the focus metric by moving in the wrong direction. Defined in [Analytics Plan](analytics.md#gateway_plugin_chain_error_ratio).

### Metric tree

The nested structure linking the focus metric to its inputs and counter-metric. Defined in [Analytics Plan](analytics.md#metric-tree).

### Event

A recorded occurrence in the product that feeds an analytics metric or an SLI. Defined in [Analytics Plan](analytics.md#events).

### Instrumentation

The code that emits events and signals from the product containers. Defined in [Analytics Plan](analytics.md#events) and [Observability Plan](observability.md#signals).

### Analytics backend

The self-hosted system that collects analytics metrics and serves dashboards. Defined in [ADR-004](architecture/adr/ADR-004.md).

### Observability backend

The self-hosted system that collects metrics and logs for SLIs. Defined in [ADR-005](architecture/adr/ADR-005.md).

### Dashboard

A visual chart in Grafana that runs the query recorded in a metric or SLI section. Defined in [Analytics Plan](analytics.md) and [Observability Plan](observability.md).

### Usability test
A structured session in which a participant performs tasks with the product while the team observes, to find where the interface or workflow fails the user.

### Participant
A person recruited to take part in a usability session. A participant is identified by a code, never by name, in any recorded or published artifact.

### Usability session
One scheduled usability test with one participant, covering the tasks, the consent, the recording, and the post-session questionnaire.

### Consent
A participant's explicit agreement to take part in a usability session and to be recorded. Consent is recorded before the session starts; a participant may withdraw it at any time.

### Recording
The audio, video, or screen capture of a usability session. The recording is private-only and is deleted when the participant withdraws consent or when the analysis is complete.

### Measure
The raw observation taken during a usability session, before any processing. A measure is a single value: a task completion time, a success flag, an error count, or a questionnaire response.

### Metric
A defined computation over one or more measures, with a stated unit and window. A metric is what the team reports: for example, the median task completion time across participants, or the proportion of sessions with a successful task.

### Statistic
A summary value computed from a sample of measures or metrics, used to describe or compare. A statistic carries uncertainty and is not the same as the metric's target; it is what the data shows, not what the product promises.

### SUS
The System Usability Scale: a ten-item questionnaire scored to a single number between 0 and 100, used to compare perceived usability across sessions or releases.

### SEQ
The Single Ease Question: a one-item questionnaire scored from 1 to 7 after each task, used to record how easy the participant found that task.