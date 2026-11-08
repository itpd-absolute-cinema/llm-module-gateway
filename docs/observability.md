# Observability Plan

This plan defines the signals the LLM Gateway emits and the service level indicators (SLIs) that make the runtime requirements measurable.

## Signals

The product uses two of the three signal types:

- **Metric:** Prometheus counters and histograms exposed by the Gateway and Plugin Manager.
- **Log:** Structured JSON lines written to stdout, scraped by the log pipeline. The diagnosability test reads the per-request log line described below.
- **Trace:** Not used in this release.

An alert is a rule over a signal, not a signal of its own.

### Structured log line

Every request produces one structured log line with these fields:

| Field | Type | Description |
| --- | --- | --- |
| `request_id` | string (opaque UUID) | Correlates all events for one request. Never derived from personal data. |
| `route` | string | The Gateway route or processing stage, e.g. `plugin_chain`, `admin_plugins`. |
| `outcome` | string | `allow`, `modify`, `block`, or `failure_policy`. |
| `duration_ms` | integer | Wall-clock time from request receipt to response, in milliseconds. |

## The SLIs

### SLI-01

Plugin chain success ratio

**Status:** Active
**Definition:** The proportion of requests that complete the plugin chain without a failure policy being applied. The **request** is identified by the `request_id` field in the structured log line, the **route** by the `route` label with value `plugin_chain`, and the **failure** by the `failure` label with value `failure_policy`. The per-request point is the structured log line carrying `request_id`, `route`, `outcome`, and `duration_ms`. The aggregate is the histogram's 5-minute P95.
**Unit:** ratio (0–1)
**Window:** 5m
**Source:** `plugin_chain_completed_total` and `failure_policy_applied_total` Prometheus counters; structured log lines.
**Query:** `sum(rate(plugin_chain_completed_total{outcome=~"allow|modify",source!="smoke"}[5m])) / sum(rate(request_received_total{source!="smoke"}[5m]))`

**Per-request log query:** `{job="gateway"} | json | route="plugin_chain" | line_format "{{.request_id}} {{.outcome}} {{.duration_ms}}"`

### SLI-02

Plugin chain error ratio

**Status:** Planned
**Definition:** The proportion of requests where a plugin failed and the Gateway applied its failure policy. Identified by the `failure_policy` label and the `plugin_name` label.
**Unit:** ratio (0–1)
**Window:** 5m
**Source:** `failure_policy_applied_total` counter.
**Query:** `sum(rate(failure_policy_applied_total{source!="smoke"}[5m])) / sum(rate(request_received_total{source!="smoke"}[5m]))`

### SLI-03

Plugin chain latency P95

**Status:** Planned
**Definition:** The 95th percentile of request duration for requests that complete the plugin chain, measured over a five-minute window.
**Unit:** milliseconds
**Window:** 5m
**Source:** `request_duration_seconds` histogram exposed by the Gateway.
**Query:** `histogram_quantile(0.95, sum by (le) (rate(request_duration_seconds_bucket{route="plugin_chain",source!="smoke"}[5m])))`
