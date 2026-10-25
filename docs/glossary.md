# Glossary

## Request and Response Processing

### Gateway

The Java service that accepts client requests, sends requests and responses through Python plugins, and communicates with LLM providers.

**Defined by:** [Architecture decision DEC-009](decisions.md#dec-009-architecture).

### Plugin

A Python file that applies a company's rules to requests, responses, or provider selection without changing the Gateway core.

**Defined by:** [Product vision](product-vision.md#goal) and [architecture decision DEC-009](decisions.md#dec-009-architecture).

### Plugin Chain

The plugins that process requests and responses in the order set by the Gateway configuration.

**Defined by:** [Functional CLI threshold](threshold-of-success.md#tos-03-functional-cli).

### Provider

An external LLM service that generates responses to requests sent by the Gateway.

**Defined by:** [Product boundary BND-01](product-vision.md#bnd-01).
