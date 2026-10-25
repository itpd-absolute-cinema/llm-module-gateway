# Plugin Chain Follow-up

This note covers the [Week 3 action points](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-03/meeting-report.md#action-points) on the plugin interface, failure handling, and monitoring.
It describes the Python experiment in [PR #39](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/39), reviewed at commit `ca30bcf8979aefb6fe0d9d870429e022214a40ed` on 25 October 2026.
The Java Gateway and Python Plugin Manager are described separately in [PR #48](https://github.com/itpd-absolute-cinema/llm-module-gateway/pull/48).
Both PRs were still open at the time of this review.

## Why Middleware

[Django middleware](https://docs.djangoproject.com/en/5.2/topics/http/middleware/) wraps the next handler: requests enter in configured order and responses return in reverse order.
A layer can return a response without calling the next handler.
A plugin can use local variables to carry information from request processing to response processing.

[Twisted Deferreds](https://docs.twisted.org/en/stable/core/howto/defer.html) provide callback and errback chains.
An exception switches processing to the error path; an errback can recover by returning a normal result or leave the failure for later handlers.
An error handler must make it clear whether processing should recover or stop.

For the Python experiment, keep the existing async middleware approach.
A plugin can do work before and after `await next_handler(...)`, with `try` and `finally` for errors and cleanup.
There is no need to add Twisted for this.
Unlike Django's per-layer exception conversion, this prototype lets unexpected exceptions unwind through outer plugins to the HTTP handler.
This explains the prototype; the product architecture is still covered by its own ADRs.

## Interface in the Prototype

The interface is defined in [core.py](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/ca30bcf8979aefb6fe0d9d870429e022214a40ed/prototype/gateway/core.py) and the [plugin example](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/ca30bcf8979aefb6fe0d9d870429e022214a40ed/prototype/README.md#add-a-plugin).

| Part | How it works |
| --- | --- |
| Factory | `factory(next_handler, **options)` returns an async handler; options are supplied when the chain is built. |
| Handler | Receives a `Context` and returns a `Response`. |
| Context | Contains the request body, request ID, and incoming authorization value. |
| Response | Contains a response body and HTTP status. |
| Configuration | A TOML entry names an importable `module:factory` and its options; changes require a restart. |
| State | Factories run once per worker; per-request state belongs in the context or handler-local variables. |
| Continuation | Await the next handler once to continue, or return a response without calling it to stop. |
| Cleanup | Use `finally` around the next-handler call for cleanup on success or failure. |

To add a plugin, create the Python module, add its factory path and options to the configuration, restart, and send a synthetic request through the local demo provider.
The linked example shows the complete factory and configuration entry.
No core change is needed.

The Week 3 discussion used a constructor, plugin ID, API version, settings model, and separate `on_request`/`on_response` hooks.
The experiment instead uses the factory import path, options, and one async handler; it has no explicit API-version field or separate hooks.
On [23 October](../decisions.md#dec-014), the customer asked for clear instructions for adding plugins.
The choice between separate hooks and one handler still needs agreement.

## Failure Handling

The [HTTP handler](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/ca30bcf8979aefb6fe0d9d870429e022214a40ed/prototype/gateway/app.py) currently behaves as follows:

| Event | Result |
| --- | --- |
| A plugin returns a rejection | Inner plugins and the provider do not run; outer plugins receive the rejection response. |
| A plugin raises an unexpected exception | The exception unwinds through outer plugins; the HTTP boundary returns a generic 500. |
| Provider timeout | The caller receives 504. |
| Provider connection failure | The caller receives 502. |
| Provider rate limit | The caller receives 429; other provider HTTP errors become a generic 502. |

The prototype does not automatically skip a failed plugin or retry a provider call.
An exception before the provider call prevents that call; a failure after the call cannot undo data already sent.
The provider timeout does not limit how long a plugin can run.

The [23 October guidance](../decisions.md#dec-013) requires detecting and logging stalled or faulty plugins while keeping the service responsive.
Returning 500 for an exception is not enough: a plugin can also hang, and the current runtime has no plugin execution deadline.
Add a time limit for plugin execution and check that other requests can still complete.
Blocking Python code may require a worker process that can be stopped, rather than an async timeout alone.

Keep rejecting the affected request on an unexpected exception until recovery is agreed.
We still need to agree whether a failed required rule can be skipped, how cleanup runs, and what error details the client receives.
Those questions remain in `reports/week-04/meeting-script.md`.

## Monitoring Still Needed

The [logging plugin](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/ca30bcf8979aefb6fe0d9d870429e022214a40ed/prototype/gateway/plugins/request_logging.py) records one request ID, status, exception class, and total elapsed time.
It omits request bodies, credentials, and exception messages.
It does not record each plugin's duration, execution order, or the identity of the failed plugin.
Per-plugin monitoring from Week 3 is still missing, including the [failure detection requested by the customer](../decisions.md#dec-013).

For each plugin call, log the request ID, plugin ID or factory path, processing stage, position in the chain, elapsed time, and result.
Record failures before re-raising them, and omit payloads and exception messages.
With nested handlers, measure the plugin's own work separately from time spent waiting for the next handler.
Test this with a plugin that deliberately fails: the log should name the plugin and stage without exposing private data.

## Checks Performed

Four local assertion checks passed against the pinned prototype sources on 25 October 2026, using Python 3.12 and synthetic data:

- Two wrapper plugins entered in configured order and unwound in reverse order.
- Literal redaction changed both request and response text; the token-cap plugin supplied its default limit.
- A disallowed model returned 403 without invoking the provider stub.
- An exception unwound through an outer handler; request logging recorded the ID, status 500, and exception class without the exception message.

These checks called the Python handlers directly with a provider stub.
HTTP behavior, per-plugin timing, Java/Python integration, and deployment were not tested.
