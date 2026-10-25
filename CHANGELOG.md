# Changelog

## Unreleased

- Document per-endpoint API errors and response headers in code and regenerate the OpenAPI snapshot; replace automatic 422 validation responses with the actual 400 contract, retaining 422 for non-TOML plugin options.

- Group documentation into six top-level menus and include individual requirements, architecture decisions, and assumptions in the navigation.

- Document prototype plugin administration, restart-only changes, targeted configuration operations, and separate admin authorization; add linked requirements, ADRs, and an OpenAPI snapshot exported from the running service.

- Add a standalone Python gateway prototype with OpenAI-compatible non-streaming text chat completions, five configurable middleware plugins, and a local demonstration provider.
