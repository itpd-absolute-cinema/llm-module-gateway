# Architecture Decisions

## Status

**Approved**: Core architecture concept.

## DEC-001 Hot reload ruled out

Install plugins by restarting or redeploying the gateway, without loading them on the fly.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Customer
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** Loading server-side plugins on the fly adds unnecessary complexity and security risks; restarting is acceptable.

## DEC-002 Data available to the plugin

Prioritize plugin access to the user request first, the LLM response second, and routing third.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Customer
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** Confirms request and response processing as the primary plugin use cases; routing is useful but lower priority.

## DEC-003 Plugin implementation language

Write plugins in a mainstream language that LLMs already know, not a new domain-specific language.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Customer
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** A new language would be harder for an LLM to generate and require extensive documentation in its context.

## DEC-004 MVP-0 shape

Make MVP-0 a proxy with a token from `.env`, one provider, and one simple masking plugin.

- **Status:** Superseded
- **Date:** 2026-10-02
- **Made by:** Team, not contested
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** A small proxy and masking example provide the first runnable demonstration of the plugin workflow.

## DEC-005

Treat a guide or skill for coding agents on writing plugins as a nice-to-have.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Team, not contested
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions); the row records explicit Customer support.
- **Why:** Focused instructions let an agent write a plugin without reading the whole codebase, but are not required for the first runnable version.

## DEC-006

Support one or two LLM providers, prioritizing depth over breadth.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Customer
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** The project demonstrates the plugin-system idea rather than comprehensive provider coverage.

## DEC-007

Start with a simple, configurable login and simple provider-key storage with a security warning.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Customer
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** Company-managed provider tokens should not be distributed to employees; simple storage keeps the first implementation small while making its security limits explicit.

## DEC-008

State the architecture's upper limit on plugins running at the same time.

- **Status:** Active
- **Date:** 2026-10-02
- **Made by:** Customer
- **Source:** [Kickoff meeting](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-01/meeting-report.md#decisions).
- **Why:** Plugin execution and messaging impose architectural limits that should be explained; industrial-grade capacity is not required.

## DEC-009 Architecture

The system consists of three components:

```text
CLI → Gateway (Java) → Python Interpreter → Plugins
```

### Gateway

- **Language:** Java
- Stateless request handling.
- Stable, long-running application with minimal restarts.
- Designed for horizontal scaling.

### Python Interpreter

- **Language:** Python
- Executes plugins, each implemented as a single `.py` file.
- Plugins reside in a dedicated directory or package.
- Adding plugins should be simple and require no Gateway changes.
- Hot-loading plugins without restarting is desirable.

### CLI

- **Language:** TBD
- Provides a terminal interface for interacting with the Gateway API.
- No GUI required.

## DEC-010 Design Principles

- **Statelessness:** Components should avoid relying on local state between requests.
- **Scalability:** Multiple instances should be deployable to distribute load.
- **Extensibility:** New functionality should be added through plugins.
- **Simplicity:** Plugin development and CLI usage should be straightforward.

## DEC-011 Tech Stack

Use Java 25 with Spring Boot 4 for the Gateway, and Python for the Interpreter, plugins, and CLI.

- **Status:** Approved by the customer
- **Date:** 2026-10-18
- **Made by:** Project team, pending confirmation
- **Source:** Part 3 – stack selection
- **Why:** Java 25 provides a modern LTS runtime for the Gateway, while Spring Boot 4 provides a framework for implementing the Gateway service. Python is used for the Interpreter and plugins because plugins must be implementable as individual `.py` files. Using Python for the CLI as well reduces the number of language ecosystems and allows the CLI to reuse Python libraries where appropriate. Gradle is used for Java build and dependency management. Docker Compose is proposed for reproducible local development and testing.

## Open Questions

- Communication protocol between Gateway and Interpreter.
- Plugin interface and lifecycle management.
- Hot-loading implementation.
- Error handling, logging, and timeouts.
- Plugin security and resource isolation.
- Deployment and load balancing strategy.
