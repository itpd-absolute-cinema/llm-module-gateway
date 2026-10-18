# Threshold of Success

**Product:** LLM Gateway  
**Target date:** 31 December 2026  
**Status:** Draft – pending customer agreement

## TOS-01 – Simple Python Plugin Development

**Goal**

By 31 December 2026, a developer can create and execute one new Python plugin by following the project's documentation and example, without modifying the Gateway core.

**Verification**

An automated test loads and executes the example plugin and verifies its expected behavior. The plugin can be added as a separate `.py` file without requiring changes to the Gateway core.

**Failure statement**

Creating a new plugin requires substantial knowledge of the Gateway internals, changes to the Gateway core, or undocumented manual steps.

**Success statement**

A developer can follow the provided documentation and example to create and execute a new plugin without modifying the Gateway core.

**Status:** Open – pending customer agreement.

## TOS-02 – Local Gateway Deployment

**Goal**

By 31 December 2026, a developer can start the LLM Gateway locally using the documented Docker workflow and successfully send a test request to the running Gateway.

**Verification**

An automated integration test starts the required Docker services, sends a test request to the Gateway, and verifies that the expected response is received.

**Failure statement**

The Gateway cannot be started locally using the documented Docker workflow, or it cannot successfully process a test request after startup.

**Success statement**

A developer can start the Gateway locally using the documented Docker workflow and successfully process a test request.

**Status:** Open – pending customer agreement.

## TOS-03 – Functional CLI

**Goal**

By the end of the ITPD course, a DevOps engineer can use the CLI to manage the LLM Gateway, register and configure Python plugins, and ensure that incoming LLM requests are processed through the configured plugin chain before the corresponding responses are returned to callers.

**Verification**

An automated integration test invokes the CLI against a running Gateway, sends a test request, and verifies that the response returned to the caller matches the expected result.

**Failure statement**

The CLI cannot successfully send a request through the Gateway or return the response to the caller.

**Success statement**

DevOps engineers can use the CLI to manage the LLM Gateway and register, configure, and load Python plugins. The Gateway processes incoming LLM requests through the configured plugin chain and returns the processed responses to the original callers.

**Status:** Open – pending customer agreement.

## TOS-04 – Plugin Chain Resilience

**Goal**

By the end of the ITPD course, a plugin failure is handled according to the documented error-handling policy, without an unhandled failure terminating the Gateway or violating the defined plugin-chain behavior.

**Verification**

Automated integration tests inject a controlled failure into a plugin and verify that:

- The Gateway remains operational.
- The error is handled according to the documented policy.
- The subsequent plugin-chain behavior matches the expected behavior for that policy.
- The test reports a failure if an unhandled exception violates the defined contract.

**Failure statement**

A plugin failure causes an unhandled Gateway failure or causes the plugin chain to behave contrary to its documented error-handling policy.

**Success statement**

Plugin failures are handled according to a documented and testable policy, and the Gateway remains operational while the plugin chain follows its defined behavior.

**Status:** Open – pending customer agreement.

## Source Failure and Success Statements

The goals above were derived from the following project-level failure and success statements.

| ID | Failure statement | Success statement |
| --- | --- | --- |
| TOS-01 | The barrier to creating a plugin is too high. | A developer can create and run a plugin by following the documentation and example, without modifying the Gateway core. |
| TOS-02 | The Gateway cannot be started locally. | The Gateway can be started locally through Docker and successfully process a test request. |
| TOS-03 | The CLI does not work. | The CLI can send a request through the Gateway and return the response. |
| TOS-04 | A plugin failure causes the plugin chain to fail contrary to the expected behavior. | Plugin failures are handled according to the documented error policy, and the Gateway remains operational. |

## Change Control

All goals are initially open and require customer agreement.

While a goal is `Open`, every change to that goal must cite the applicable customer decision using the `DEC-nnn` identifier. Dropping a goal requires agreement with the customer.

No customer decision identifier or approval is recorded in this draft. The goals must be reviewed and agreed upon with the course curator before they are treated as approved thresholds of success.
