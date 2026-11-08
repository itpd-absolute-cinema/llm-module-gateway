# Deployment

| Environment | Purpose |
| --- | --- |
| Development | Local build and checks; see [setup](setup.md). |
| Integration | Product checks in GitHub Actions (pending). |
| Staging | Release candidate on the university VM, port 8081. |
| Production | Customer deployment on the university VM, port 8080. |

The university VM is reachable over SSH from the university network. A team
member on campus deploys and verifies it. Staging and production
addresses are given to the customer through Moodle; the production address has the form
`http://<university-vm-ipv4>:8080`.

## Configuration

On the VM, keep separate checkouts at `/opt/llm-gateway-staging` and
`/opt/llm-gateway-production`, each with its own ignored `.env` file.
Use the shape in `.env.example`, with `COMPOSE_PROJECT_NAME` set to
`gateway-staging` and `gateway-production`, ports 8081 and 8080 respectively,
and a different randomly generated `GATEWAY_ADMIN_API_KEY` in each.
Restrict file permissions to the operator.

The project names isolate networks and volumes.
Only the gateway port is exposed; the admin API shares that port, permits
uploading executable plugins, and must be accessible only to trusted operators.
The current provider is a stub, not a live LLM.
When deploying to a VM, set `IMAGE_TAG` in both files to the same `sha-<short-commit>` tag published by
the image workflow, since `latest` can change and is unsuitable for a release.

## Deploy, verify and roll back

In each checkout on the VM, fetch the candidate and check out its exact commit.
Record the previous production commit and back up its database and plugin
volumes before changing a running deployment. Use [setup](setup.md) for the
pull and run commands from the staging checkout.

Verify the core request and
plugin flow from a campus machine. Repeat from the production checkout using the
same image tag. Verify the core task again and
record the result with the release. If verification fails, restore the previous
commit, image tag and backed-up data, redeploy it, and verify again.

## Initial verification

On 31 October 2026, staging and production ran the `sha-47c725e` application
images with deployment configuration from commit `ba21cd9`. From a campus
machine, a chat request containing a made-up phone number returned
`[stub] Call [PHONE]` in each environment after enabling the `mask_phone`
request filter. Both Plugin Manager health checks passed. The provider was a
stub, so this did not test a real LLM. Carry this evidence into the release
record when REL-05 is published.

## Analytics Pipeline

The metrics and events the product emits are defined in [Analytics Plan](analytics.md). This section describes the pipeline that collects them.

| Stage | Component | Purpose |
| --- | --- | --- |
| Emission | Gateway (Java), Plugin Manager (Python) | Expose Prometheus counters and histograms on `/metrics`. |
| Collection | Prometheus on the university VM | Scrapes the staging and production endpoints. |
| Visualisation | Grafana on the university VM | Renders the metric tree defined in `docs/analytics.md`. |

The analytics backend is self-hosted on the university VM and is not part of the product. The product works with collection off; the backend is an external system in the Container View.

### Environment scopes

Staging and production each have their own Prometheus scrape job and their own Grafana datasource, with a separate credential. The staging scope receives the `source=smoke` test events; the production scope does not.

### Configuration

On the VM, the analytics backend reads its configuration from an ignored `.env` file with the shape in `.env.example`. The values for `PROMETHEUS_URL`, `GRAFANA_URL`, and `GRAFANA_CREDENTIAL` are private-only and are never committed. The scrape interval and the exporter interval are recorded in `.env.example` as their defaults.

### Deployment steps

1. From a campus machine, SSH to the university VM.
2. Start Prometheus and Grafana from the documented compose files, using the staging and production environment files.
3. Confirm that Prometheus scrapes the Gateway and Plugin Manager `/metrics` endpoints in both environments.
4. Open the Grafana dashboard for each environment and confirm the focus-metric chart runs the query recorded in `docs/analytics.md`.
5. Send a staging `source=smoke` event and confirm it appears in the event list but not in the metric chart.
6. Leave the production dashboard configured with the same query and unexercised.

The backend and dashboard addresses and credentials are given to the customer through Moodle; they are not recorded in this repository.

## Observability Pipeline

The signals the product emits and the SLIs they feed are defined in [Observability Plan](observability.md). This section describes the pipeline that collects them.

| Stage | Component | Purpose |
| --- | --- | --- |
| Emission | Gateway (Java), Plugin Manager (Python) | Expose Prometheus metrics on `/metrics` and write structured JSON log lines to stdout. |
| Collection | Prometheus on the university VM | Scrapes the staging and production `/metrics` endpoints. |
| Collection | Log store on the university VM | Collects the structured log lines from the Gateway and Plugin Manager. |
| Visualisation | Grafana on the university VM | Renders the SLI dashboard defined in `docs/observability.md`. |

The observability backend is self-hosted on the university VM and is not part of the product. The product works with collection off; the backend is an external system in the Container View.

### Environment scopes

Staging and production each have their own Prometheus scrape job, log stream, and Grafana datasource, with a separate credential. The staging scope receives the `source=smoke` test events; the production scope does not.

### Configuration

On the VM, the observability backend reads its configuration from an ignored `.env` file with the shape in `.env.example`. The values for `PROMETHEUS_URL`, `LOG_STORE_URL`, `GRAFANA_URL`, and `GRAFANA_CREDENTIAL` are private-only and are never committed. The log exporter batch interval and the metric scrape interval are recorded in `.env.example` as their defaults.

### Deployment steps

1. From a campus machine, SSH to the university VM.
2. Start Prometheus, the log store, and Grafana from the documented compose files, using the staging and production environment files.
3. Confirm that Prometheus scrapes the Gateway and Plugin Manager `/metrics` endpoints in both environments, and that structured log lines reach the log store.
4. Open the Grafana dashboard for each environment and confirm the chart for the Active SLI runs the query recorded in `docs/observability.md`.
5. Send a staging request that fails during plugin processing. Confirm that the SLI chart shows a separate failure series with a numeric P95, and that the per-request log point with `request_id`, `route`, and `failure` label appears in the log store.
6. Send a staging `source=smoke` event and confirm it appears in the event list but not in the SLI chart.
7. Leave the production dashboard configured with the same query and unexercised.

The backend and dashboard addresses and credentials are given to the customer through Moodle; they are not recorded in this repository.