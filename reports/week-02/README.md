## Minimum Usable Product Candidate

Core task: a client sends a request to the Java gateway, the request passes through a chain of Python plugins (request filter → routing → response filter), and the client receives a processed response from the target LLM.

- [`US-XX`: Register and load a Python plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/17)
- [`US-XX`: Filter the request before routing](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/18)
- [`US-XX`: Route a request to a target LLM via a plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/19)
- [`US-XX`: Filter the response before returning it to the client](https://github.com/itpd-absolute-cinema/llm-module-gateway/issues/20)

Customer's verdict: [`DEC-nnn`](../../docs/decisions.md#dec-nnn).
