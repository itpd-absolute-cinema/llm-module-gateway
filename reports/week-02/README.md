# Minimum Usable Product Candidate

Core task: a client sends a request to the Java gateway, the request passes through a chain of Python plugins (request filter → routing → response filter), and the client receives a processed response from the target LLM.

- [`US-01`: Register and load a Python plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/17)
- [`US-02`: Filter the request before routing](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/18)
- [`US-03`: Route a request to a target LLM via a plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/19)
- [`US-04`: Filter the response before returning it to the client](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/20)

Customer's verdict: [`DEC-005`](../../docs/decisions.md).
