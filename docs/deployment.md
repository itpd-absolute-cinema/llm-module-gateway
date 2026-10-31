# Deployment

| Environment | Purpose |
| --- | --- |
| Development | Local build and checks; see [setup](setup.md). |
| Integration | Product checks in GitHub Actions (pending). |
| Staging | Release candidate on the university VM, port 8081. |
| Production | Customer deployment on the university VM, port 8080. |

The university VM is reachable over SSH from the university network. A team
member on campus deploys and verifies it. Give the actual staging and production
addresses to the customer through Moodle; the production address has the form
`http://<university-vm-ipv4>:8080`.

## Configuration

On the VM, keep separate environment files outside the checkout, for example
`/root/gateway-config/staging.env` and `/root/gateway-config/production.env`.
Use the shape in `.env.example`, with ports 8081 and 8080 respectively and a
different randomly generated `GATEWAY_ADMIN_API_KEY` in each. Restrict file
permissions to the operator. Compose project names isolate networks and volumes.
Only the gateway port is exposed; the admin API shares that port, permits
uploading executable plugins, and must be accessible only to trusted operators.
The current provider is a stub, not a live LLM.
Set `IMAGE_TAG` in both files to the same `sha-<short-commit>` tag published by
the image workflow. Authenticate Docker to GHCR if the packages are private.

## Deploy, verify and roll back

From a checkout on the VM, fetch the candidate and check out its exact commit.
Record the previous production commit and back up its database and plugin
volumes before changing a running deployment. Use [setup](setup.md) for the
pull and run commands, adding `--env-file /root/gateway-config/staging.env -p
gateway-staging` immediately after `docker compose`. Verify the core request
and plugin flow from a campus machine. Repeat with `production.env` and project
`gateway-production`, using the same image tag. Verify the core task again and
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
