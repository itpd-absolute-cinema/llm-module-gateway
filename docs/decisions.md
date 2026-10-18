# Architecture Decisions

## Status

**Approved**: Core architecture concept.

## DEC-001 Architecture

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

## DEC-002 Design Principles

- **Statelessness:** Components should avoid relying on local state between requests.
- **Scalability:** Multiple instances should be deployable to distribute load.
- **Extensibility:** New functionality should be added through plugins.
- **Simplicity:** Plugin development and CLI usage should be straightforward.

## DEC-003 Open Questions

- Communication protocol between Gateway and Interpreter.
- Plugin interface and lifecycle management.
- Hot-loading implementation.
- Error handling, logging, and timeouts.
- Plugin security and resource isolation.
- Deployment and load balancing strategy.

## DEC-004 Next Steps

1. Define the Gateway API and inter-component protocol.
2. Specify the plugin interface.
3. Implement a minimal end-to-end prototype.
4. Evaluate hot-loading and horizontal scaling.

## DEC-005 Tech Stack

Use Java 25 with Spring Boot 4 for the Gateway, and Python for the Interpreter, plugins, and CLI.

- **Status:** Approved by the customer
- **Date:** 2026-10-18
- **Made by:** Project team, pending confirmation
- **Source:** Part 3 – stack selection
- **Why:** Java 25 provides a modern LTS runtime for the Gateway, while Spring Boot 4 provides a framework for implementing the Gateway service. Python is used for the Interpreter and plugins because plugins must be implementable as individual `.py` files. Using Python for the CLI as well reduces the number of language ecosystems and allows the CLI to reuse Python libraries where appropriate. Gradle is used for Java build and dependency management. Docker Compose is proposed for reproducible local development and testing.
