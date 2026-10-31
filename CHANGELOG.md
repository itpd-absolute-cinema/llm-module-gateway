# Changelog

## [Unreleased]

### Added

- Isolated staging and production deployments using the CI container images.
- Rendered Gateway and Plugin Manager OpenAPI references in the documentation.

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
