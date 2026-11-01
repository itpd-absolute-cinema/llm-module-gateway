# Changelog

## [Unreleased]

## [0.1.0](https://github.com/itpd-absolute-cinema/llm-module-gateway/releases/tag/v0.1.0) - 2026-10-25

### Added

- Standalone Python gateway prototype with OpenAI-compatible non-streaming text chat, five middleware plugins, and a local demonstration provider.
- Separately authorized plugin-management API for uploading, configuring, ordering, enabling, disabling, and deleting plugins. Changes take effect after restart; GET reports running configuration and pending-restart flags.
- API reference and OpenAPI snapshot, with documented error responses and headers.
- Functional and quality requirements, architecture decisions, and plugin-management documentation.

### Changed

- Group documentation into six top-level menus with individual requirements, decisions, and assumptions in the navigation.

### Fixed

- OpenAPI validation responses now describe the actual HTTP 400 contract instead of FastAPI's automatic 422 response; non-TOML plugin options retain HTTP 422.

# Changelog

## [Unreleased]

### Added

- Isolated staging and production deployments using the CI container images.
- Rendered Gateway and Plugin Manager OpenAPI references in the documentation.

## [0.2.0](https://github.com/itpd-absolute-cinema/llm-module-gateway/releases/tag/v0.2.0) - 2026-10-31

### Added

- CLI implementation providing a command-line interface for interacting with the LLM Gateway system.
- LLM Gateway functionality, integrating the core gateway components and supporting the system's request-processing workflow.
- Plugin Manager implementation, enabling plugin management through the dedicated management interface.
- Docker-based local development environment with Docker Compose for simplified project setup and execution.
- Automated tests covering the implemented functionality and core system components.
- Continuous Integration pipeline with automated test execution.
- Virtual machine deployment of the integrated system.
- Updated project documentation covering the implemented components, setup instructions, and development workflows.

### Changed

- Integrated the CLI, Gateway, and Plugin Manager into a functional system.
- Improved project configuration and startup procedures to support consistent local development and deployment.
- Expanded and updated technical documentation to reflect the current system implementation.

### Fixed

- Addressed issues identified during implementation and integration of the system components.
- Improved application startup and configuration consistency across local and deployed environments.