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

## Deploy, verify and roll back

From a checkout on the VM, fetch the candidate and check out its exact commit.
Record the previous production commit and back up its database and plugin
volumes before changing a running deployment. Use [setup](setup.md) for build
and run details; set the Compose project to `gateway-staging` and pass its
environment file with `--env-file`. Verify the core request and plugin flow
from a campus machine. Deploy the same commit with project `gateway-production`
and its environment file, then verify the core task again and record the result
with the release. If verification fails, restore the previous commit and its
backed-up data, redeploy it, and verify again.
