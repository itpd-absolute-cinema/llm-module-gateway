# User Acceptance Tests

Index of the customer's acceptance tests. Each test is a customer journey that the customer runs on the deployed product and judges `Pass`, `Fail`, or `Not run`.

## The Tests

| ID | Journey | Status |
|---|---|---|
| [UAT-01](#uat-01) | A DevOps engineer creates a plugin and configures it in the plugin chain | Planned |
| [UAT-02](#uat-02) | A DevOps engineer creates a user | Planned |
| [UAT-03](#uat-03) | A coder connects the AC Gateway to OpenCode | Planned |

## UAT-01

**Journey:** A DevOps engineer creates a plugin and configures it in the plugin chain.

**Start state:** The web UI is open on the production instance, logged in as an operator. The plugin list is empty for the team's test workspace. The staging seed is reset to synthetic data.

**Steps:**

1. Open the plugin management area.
2. Create a new plugin that masks phone numbers in requests.
3. Enable the plugin.
4. Place the plugin in the request chain, select the position.
5. Save the configuration.

**Expected outcome:** The new plugin appears in the plugin list with status `enabled`, and it appears in the request chain in the position the operator chose. A request with a phone number is masked before it reaches the model.

## UAT-02

**Journey:** A DevOps engineer creates a user.

**Start state:** The web UI is open on the production instance, logged in as an operator. The users list contains only the operator's own account.

**Steps:**

1. Open the user management area.
2. Create a new user with a name and an email.
3. Set the user's access to the gateway.
4. Save the user.

**Expected outcome:** The new user appears in the users list with the access the operator set, and the operator can state how the new user would log in.

## UAT-03

**Journey:** A coder connects the AC Gateway to OpenCode using the configuration and setup instructions the DevOps engineer provides.

**Start state:** The Gateway is deployed on the production instance with at least one enabled plugin. The DevOps engineer has created a user for the coder in UAT-02. The coder has OpenCode installed on their machine.

**Steps:**

1. The DevOps engineer opens the gateway console and copies the OpenCode configuration and the setup instructions.
2. The DevOps engineer sends the configuration and the instructions to the coder.
3. The coder follows the setup instructions to configure OpenCode as a provider.
4. The coder signs in with the credential the DevOps engineer provided.
5. The coder sends a request from OpenCode.

**Expected outcome:** OpenCode receives a response through the gateway, the request passes through the enabled plugin chain, and the request appears in the gateway's usage dashboard with the coder's user.

## The Runs

| UAT | Sprint | Date | Runner | Reviewed build | Result | Evidence | Decision |
|---|---|---|---|---|---|---|---|
| UAT-01 | SPRINT-09 | 27.11.2026 | Customer | <build> | <Pass / Fail / Not run> | [meeting report](../reports/week-09/meeting-report.md#user-acceptance-tests) | DEC-13 |
| UAT-02 | SPRINT-09 | 27.11.2026 | Customer | <build> | <Pass / Fail / Not run> | [meeting report](../reports/week-09/meeting-report.md#user-acceptance-tests) | DEC-14 |
| UAT-03 | SPRINT-09 | 27.11.2026 | Customer | <build> | <Pass / Fail / Not run> | [meeting report](../reports/week-09/meeting-report.md#user-acceptance-tests) | DEC-15 |