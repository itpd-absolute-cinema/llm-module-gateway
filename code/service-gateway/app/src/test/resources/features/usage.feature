Feature: Usage accounting and pricing
  Every request to /chat/completions leaves an event: tokens, latency, status and the cost at the
  moment of the request. In tests the LLM is the stub: for "Hello" it reports 1 prompt token and
  2 completion tokens. Model "stub-error" fails, "stub-no-usage" sends no usage in a stream.

  Background:
    Given I am authenticated as admin
    And the model "gpt-test" costs 1000000 per 1M input tokens and 2000000 per 1M output tokens

  # ---------- regular requests ----------

  Scenario: A successful request is recorded with tokens, latency and cost
    When I ask the model "gpt-test" the question "Hello"
    And I remember the request id of the response
    And I list the usage events
    Then the response status is 200
    And the response field "$.total" is "1"
    And the response field "$.items[0].user_id" is "admin-1"
    And the response field "$.items[0].model" is "gpt-test"
    And the response field "$.items[0].status" is "ok"
    And the response field "$.items[0].prompt_tokens" is "1"
    And the response field "$.items[0].completion_tokens" is "2"
    And the response field "$.items[0].total_tokens" is "3"
    And the response field "$.items[0].usage_source" is "upstream"
    And the response field "$.items[0].stream" is "false"
    And the response field "$.items[0].cost" is approximately 5.0
    And the response field "$.items[0].currency" is "USD"
    And the response field "$.items[0].latency_ms" is not null
    And the first usage event has the remembered request id

  Scenario: Changing the price does not rewrite history
    When I ask the model "gpt-test" the question "Hello"
    And the model "gpt-test" costs 5000000 per 1M input tokens and 5000000 per 1M output tokens
    And I list the usage events
    Then the response field "$.items[0].cost" is approximately 5.0

  Scenario: A model without a price is recorded without cost
    When I ask the model "unpriced" the question "Hello"
    And I list the usage events
    Then the response field "$.items[0].status" is "ok"
    And the response field "$.items[0].total_tokens" is "3"
    And the response field "$.items[0].cost" is "null"

  # ---------- unsuccessful requests ----------

  Scenario: A request rejected by a request filter is recorded
    Given the following plugins exist:
      | name     | type           | enabled |
      | validate | request_filter | true    |
    And the plugin manager rejects requests with status 400 and message "Prompt is too long"
    When I ask the model "gpt-test" the question "Hello"
    And I list the usage events
    Then the response field "$.items[0].status" is "rejected_by_plugin"
    And the response field "$.items[0].error_code" is "plugin_rejected"
    And the response field "$.items[0].total_tokens" is "0"
    And the response field "$.items[0].usage_source" is "none"

  Scenario: A response rejected by a response filter still counts the tokens the LLM spent
    Given the following plugins exist:
      | name | type            | enabled |
      | nsfw | response_filter | true    |
    And the plugin manager rejects responses with status 403 and message "Answer is not allowed"
    When I ask the model "gpt-test" the question "Hello"
    And I list the usage events
    Then the response field "$.items[0].status" is "rejected_by_plugin"
    And the response field "$.items[0].total_tokens" is "3"

  Scenario: An upstream failure is recorded
    When I ask the model "stub-error" the question "Hello"
    Then the response status is 502
    And the error code is "upstream_error"
    When I list the usage events
    Then the response field "$.items[0].status" is "error"
    And the response field "$.items[0].error_code" is "upstream_error"

  Scenario: A request of a blocked user is recorded
    When I block the user "user-1" with the reason "Abuse"
    And I am authenticated as a user without the admin role
    And I ask the model "gpt-test" the question "Hello"
    And I am authenticated as admin
    And I list the usage events with status "blocked"
    Then the response field "$.total" is "1"
    And the response field "$.items[0].user_id" is "user-1"
    And the response field "$.items[0].error_code" is "user_blocked"

  # ---------- streaming ----------

  Scenario: A stream reports usage in its last chunk, the gateway records it
    When I stream the question "Hello" to the model "gpt-test" asking for usage
    Then the response status is 200
    And the response header "Content-Type" starts with "text/event-stream"
    And the response body contains "[stub] "
    And the response body contains "\"usage\""
    And the response body contains "data: [DONE]"
    When I list the usage events
    Then the response field "$.items[0].stream" is "true"
    And the response field "$.items[0].status" is "ok"
    And the response field "$.items[0].prompt_tokens" is "1"
    And the response field "$.items[0].completion_tokens" is "2"
    And the response field "$.items[0].usage_source" is "upstream"
    And the response field "$.items[0].cost" is approximately 5.0

  Scenario: The usage chunk is not forwarded to a client that did not ask for it, but is still counted
    When I stream the question "Hello" to the model "gpt-test"
    Then the response status is 200
    And the response body contains "data: [DONE]"
    And the response body does not contain "\"usage\""
    When I list the usage events
    Then the response field "$.items[0].total_tokens" is "3"
    And the response field "$.items[0].usage_source" is "upstream"

  Scenario: Without usage from the upstream the gateway counts tokens itself
    When I stream the question "Hello" to the model "stub-no-usage" asking for usage
    Then the response status is 200
    And the response body does not contain "\"usage\""
    When I list the usage events
    Then the response field "$.items[0].usage_source" is "estimated"
    And the response field "$.items[0].total_tokens" is greater than 0
    And the response field "$.items[0].status" is "ok"

  Scenario: Streaming is refused while response filters are enabled
    Given the following plugins exist:
      | name | type            | enabled |
      | nsfw | response_filter | true    |
    When I stream the question "Hello" to the model "gpt-test"
    Then the response status is 400
    And the error code is "stream_not_supported_with_response_filters"
    When I list the usage events
    Then the response field "$.items[0].status" is "error"
    And the response field "$.items[0].stream" is "true"

  Scenario: An upstream error before the stream starts is a normal JSON error
    When I stream the question "Hello" to the model "stub-error"
    Then the response status is 502
    And the error code is "upstream_error"

  # ---------- reports ----------

  Scenario: The summary adds up tokens, cost and errors
    When I ask the model "gpt-test" the question "Hello"
    And I ask the model "gpt-test" the question "Hello"
    And I ask the model "stub-error" the question "Hello"
    And I request the usage summary
    Then the response status is 200
    And the response field "$.requests" is "3"
    And the response field "$.errors" is "1"
    And the response field "$.error_rate" is approximately 0.3333
    And the response field "$.total_tokens" is "6"
    And the response field "$.cost" is approximately 10.0
    And the response field "$.currency" is "USD"
    And the response field "$.by_status.ok" is "2"
    And the response field "$.by_status.error" is "1"

  Scenario: Events can be filtered
    When I ask the model "gpt-test" the question "Hello"
    And I ask the model "stub-error" the question "Hello"
    And I request GET "/admin/usage/events?status=error"
    Then the response field "$.total" is "1"
    When I request GET "/admin/usage/events?user_id=nobody"
    Then the response field "$.total" is "0"
    When I request GET "/admin/usage/events?model=gpt-test&size=1"
    Then the response field "$.total" is "1"

  Scenario: Events are exported as CSV
    When I ask the model "gpt-test" the question "Hello"
    And I request GET "/admin/usage/export.csv"
    Then the response status is 200
    And the response header "Content-Disposition" starts with "attachment"
    And the response body contains "timestamp,request_id,user_id,model,status"
    And the response body contains "admin-1,gpt-test,ok"

  Scenario Outline: Bad query parameters are rejected
    When I request GET "<path>"
    Then the response status is 400

    Examples:
      | path                                                                |
      | /admin/usage/events?from=2026-12-01&to=2026-11-01                   |
      | /admin/usage/events?from=yesterday                                  |
      | /admin/usage/events?status=bogus                                    |
      | /admin/usage/events?size=0                                          |
      | /admin/usage/events?page=abc                                        |
      | /admin/usage/timeseries?interval=week                               |
      | /admin/usage/timeseries?group_by=color                              |
      | /admin/usage/timeseries?interval=hour&from=2020-01-01&to=2026-01-01 |

  # ---------- pricing ----------

  Scenario: The price table can be read and replaced
    When I replace the pricing with:
      """
      {"items": [
        {"model": "a", "input_price_per_1m": 1.5, "output_price_per_1m": 3, "currency": "EUR"},
        {"model": "b", "input_price_per_1m": 0, "output_price_per_1m": 0, "currency": "EUR"}]}
      """
    Then the response status is 200
    And the response field "$.currency" is "EUR"
    When I request GET "/admin/pricing"
    Then the response field "$.items[0].model" is "a"
    And the response field "$.items[0].input_price_per_1m" is approximately 1.5
    And the response field "$.items[0].updated_at" is not null
    And the response field "$.items[1].model" is "b"

  Scenario Outline: Invalid price tables are rejected
    When I replace the pricing with:
      """
      <body>
      """
    Then the response status is <status>
    And the error code is "<code>"

    Examples:
      | body                                                                                                                                                                  | status | code             |
      | {"items": [{"model": "a", "input_price_per_1m": -1, "output_price_per_1m": 1, "currency": "USD"}]}                                                                       | 400    | malformed_request |
      | {"items": [{"model": "a", "input_price_per_1m": 1, "output_price_per_1m": 1, "currency": "usd"}]}                                                                        | 400    | malformed_request |
      | {"items": [{"model": "a", "input_price_per_1m": 1, "output_price_per_1m": 1, "currency": "USD"}, {"model": "a", "input_price_per_1m": 2, "output_price_per_1m": 2, "currency": "USD"}]} | 400    | duplicate_model   |
      | {"items": [{"model": "a", "input_price_per_1m": 1, "output_price_per_1m": 1, "currency": "USD"}, {"model": "b", "input_price_per_1m": 2, "output_price_per_1m": 2, "currency": "EUR"}]} | 422    | mixed_currencies  |

  # ---------- models ----------

  Scenario: Models are listed in the OpenAI format
    Given I am authenticated as a user without the admin role
    When I request GET "/models"
    Then the response status is 200
    And the response field "$.object" is "list"
    And the response field "$.data[0].id" is "stub-model"
    And the response field "$.data[0].object" is "model"

  Scenario: The admin model list carries prices
    When I request GET "/admin/models"
    Then the response status is 200
    And the response has no field "$.items[0].pricing"
    And the response field "$.items[1].id" is "gpt-test"
    And the response field "$.items[1].pricing.currency" is "USD"
    And the response field "$.items[1].pricing.input_price_per_1m" is approximately 1000000.0
