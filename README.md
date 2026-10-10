# Modular LLM Gateway

[GitHub](https://github.com/itpd-absolute-cinema/llm-module-gateway)

**Team:** Team 1 - Absoulute Cinema

A modular LLM Gateway designed to provide company-specific policies and controls when employees use Large Language Models (LLMs).

Overview

Existing LLM gateways may not fit specific company requirements, such as applying custom rules for filtering sensitive information. This project aims to provide a flexible gateway where functionality can be extended through plugins.

The gateway consists of a core and a set of common plugins. Plugins are designed to be independently developed and extensible, including with the help of coding agents. The project will provide enough documentation and context for developers to create their own plugins for specific gateway policies and use cases.

The gateway is intended to be deployed on a VPS and used by company employees subject to the organization's gateway policies.

## Documentation

* [Week 01 Report](reports/week-01/README.md)
* [Project Documentation](docs/)

## Markdown checks

Run the Markdown check from the repository root with Node.js installed:

```sh
npx --yes markdownlint-cli2@0.23.2 '**/*.md'
```

This uses the same tool version and configuration as the Markdown workflow.
Both the Markdown and link checks run on pull requests and pushes to `main`.

## Status

This project is a work in progress developed as part of the ITPD course.
