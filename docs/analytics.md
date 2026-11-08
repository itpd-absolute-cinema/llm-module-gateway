# Analytics Plan

This plan defines the metrics the team uses to judge whether the LLM Gateway delivers its value proposition. Metrics are exposed by the Gateway and Plugin Manager, scraped by Prometheus, and visualized in Grafana.

**Focus metric:** `gateway_plugin_chain_completeness_ratio` — expresses [VP-01](product-vision.md#vp-01).

## Metric Tree

- **[gateway_plugin_chain_completeness_ratio](#gateway_plugin_chain_completeness_ratio)**
  - **[L1 (counter-metric): gateway_plugin_chain_error_ratio](#gateway_plugin_chain_error_ratio)**
  - **[L1 input: gateway_plugin_execution_success_total](#gateway_plugin_execution_success_total)**
    - [L2 input: gateway_plugin_upload_success_total](#gateway_plugin_upload_success_total)
    - [L2 input: gateway_plugin_enable_success_total](#gateway_plugin_enable_success_total)

## Why This Focus Metric

The focus metric may have no meaningful value until people outside the team use the product, because it measures production request behavior.

- **Easy to understand:** a ratio between 0 and 1 of requests that passed cleanly through the full enabled plugin chain.
- **Customer-centric:** reflects whether the Gateway actually filters and validates requests as promised.
- **Sustainable value:** stable high completeness means users can trust the plugin chain over time.
- **Vision alignment:** directly measures the product goal "Filter and validate LLM requests and responses."
- **Quantitative:** PromQL ratio, tracked continuously.
- **Actionable:** the team can fix plugin crashes, validation bugs, or registry desync to move it.
- **Leading indicator:** falling completeness predicts user-visible failures before support tickets arrive.

**Rejected vanity metric:** `gateway_requests_total` — total requests processed. It grows with traffic regardless of whether the plugin chain works. A spike tells the team nothing to fix; it is a vanity metric [citation:12].

## gateway_plugin_chain_completeness_ratio

**Definition:** The fraction of requests in which every enabled plugin executed once on the request path and once on the response path, without a failure policy being triggered.
**Source:** Gateway request logs and plugin execution events, aggregated in Prometheus.
**Query:** `sum(rate(gateway_plugin_chain_complete_total[5m])) / sum(rate(gateway_requests_total[5m]))`
This metric is computed by its query, not by hand.

## gateway_plugin_chain_error_ratio

**Definition:** The fraction of requests where at least one enabled plugin raised an error or timed out, causing the Gateway to apply its failure policy.
**Source:** Gateway failure-policy log events.
**Query:** `sum(rate(gateway_plugin_chain_failed_total[5m])) / sum(rate(gateway_requests_total[5m]))`

## gateway_plugin_execution_success_total

**Definition:** Total successful plugin executions across all enabled plugins.
**Source:** Plugin Manager execution counter, exposed via `/metrics`.
**Query:** `sum(rate(gateway_plugin_execution_success_total[5m]))`

## gateway_plugin_upload_success_total

**Definition:** Total plugin uploads that completed with a valid plugin file and registered name.
**Source:** Gateway admin API success responses.
**Query:** `sum(rate(gateway_plugin_upload_success_total[5m]))`

## gateway_plugin_enable_success_total

**Definition:** Total enable operations that successfully marked a plugin as enabled.
**Source:** Gateway admin API success responses.
**Query:** `sum(rate(gateway_plugin_enable_success_total[5m]))`

## Events

| Event | Fires when | Fields | Collected at |
|---|---|---|---|
| `plugin_upload_requested_total` | DevOps calls `cli plugin upload` | `source`, `plugin_name`, `file_size_bytes` | Gateway (Java), Prometheus scrape |
| `plugin_upload_completed_total` | Gateway writes the plugin file to the shared volume | `source`, `plugin_name`, `success` | Gateway (Java), Prometheus scrape |
| `plugin_enabled_total` | DevOps calls `cli plugin enable` | `source`, `plugin_name` | Gateway (Java), Prometheus scrape |
| `plugin_disabled_total` | DevOps calls `cli plugin disable` | `source`, `plugin_name` | Gateway (Java), Prometheus scrape |
| `request_received_total` | A client sends an LLM request | `source`, `model` | Gateway (Java), Prometheus scrape |
| `plugin_chain_started_total` | Gateway begins the plugin chain | `source`, `enabled_plugin_count` | Gateway (Java), Prometheus scrape |
| `plugin_executed_total` | A plugin finishes execution | `source`, `plugin_name`, `success` | Plugin Manager (Python), Prometheus scrape |
| `plugin_chain_completed_total` | The chain returns allow/modify/block | `source`, `decision` | Gateway (Java), Prometheus scrape |
| `failure_policy_applied_total` | A plugin fails or times out and the Gateway applies a policy | `source`, `plugin_name`, `policy` | Gateway (Java), Prometheus scrape |
| `smoke_test_total` | Staging pipeline checks itself | `source=smoke`, `test_id` | Gateway (staging), Prometheus scrape |

**Naming convention:** `<domain>_<object>_<action>_total` in snake_case. `source` is a closed set: `gateway`, `plugin_manager`, `cli`, `smoke`. Only events with `source=smoke` are excluded from metrics.

**Opaque identifier:** `request_id` (random UUID, not derived from personal data, not reversible to a person) appears only in structured logs for correlation, never in Prometheus labels.