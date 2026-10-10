# Architecture Decisions

## Status

**Approved**: Core architecture concept.

## 1. Architecture

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

## 2. Design Principles

- **Statelessness:** Components should avoid relying on local state between requests.
- **Scalability:** Multiple instances should be deployable to distribute load.
- **Extensibility:** New functionality should be added through plugins.
- **Simplicity:** Plugin development and CLI usage should be straightforward.

## 3. Open Questions

- Communication protocol between Gateway and Interpreter.
- Plugin interface and lifecycle management.
- Hot-loading implementation.
- Error handling, logging, and timeouts.
- Plugin security and resource isolation.
- Deployment and load balancing strategy.

## 4. Next Steps

1. Define the Gateway API and inter-component protocol.
2. Specify the plugin interface.
3. Implement a minimal end-to-end prototype.
4. Evaluate hot-loading and horizontal scaling.
