Feature: Chat completions
  The gateway runs the active request filters, calls the LLM and then runs
  the active response filters. In tests the LLM is the built-in stub, which
  answers "[stub] <last user message>", and the plugin manager is a test double.

  Background:
    Given I am authenticated as admin

  Scenario: Without active plugins the request goes straight to the LLM
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is 200
    And the response field "$.object" is "chat.completion"
    And the response field "$.model" is "gpt-test"
    And the response field "$.choices[0].message.role" is "assistant"
    And the response field "$.choices[0].message.content" is "[stub] Hello"
    And the response field "$.choices[0].finish_reason" is "stop"
    And the plugin manager received no request filters
    And the plugin manager received no response filters

  Scenario: Only enabled plugins of the right type are sent, in chain order
    Given the following plugins exist:
      | name     | type            | enabled |
      | validate | request_filter  | true    |
      | router   | request_filter  | false   |
      | mask     | request_filter  | true    |
      | nsfw     | response_filter | true    |
      | tone     | response_filter | false   |
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is 200
    And the request filters received by the plugin manager are, in order:
      | validate |
      | mask     |
    And the response filters received by the plugin manager are, in order:
      | nsfw |

  Scenario: Moving a plugin changes the order in which filters are applied
    Given the following plugins exist:
      | name | type           | enabled |
      | a    | request_filter | true    |
      | b    | request_filter | true    |
    When I patch the plugin "b" with:
      """
      {"before": "a"}
      """
    And I ask the model "gpt-test" the question "Hello"
    Then the request filters received by the plugin manager are, in order:
      | b |
      | a |

  Scenario: Plugin settings are sent with every request
    Given the following plugins exist:
      | name | type           | enabled | options              |
      | mask | request_filter | true    | {"replacement": "*"} |
    When I ask the model "gpt-test" the question "Hello"
    Then the options passed to the plugin manager for the request filter "mask" contain "replacement" equal to "*"

  Scenario: A request filter can mask data before it reaches the LLM
    Given the following plugins exist:
      | name | type           | enabled |
      | mask | request_filter | true    |
    And the plugin manager masks phone numbers in requests
    When I ask the model "gpt-test" the question "Call me at +7 999 123-45-67 please"
    Then the response status is 200
    And the response field "$.choices[0].message.content" is "[stub] Call me at [PHONE_MASK] please"

  Scenario: A request filter can route the request to another model
    Given the following plugins exist:
      | name   | type           | enabled |
      | router | request_filter | true    |
    And the plugin manager routes requests to the model "cheap-model"
    When I ask the model "gpt-test" the question "Hello"
    Then the response field "$.model" is "cheap-model"

  Scenario: A response filter can change the LLM answer
    Given the following plugins exist:
      | name | type            | enabled |
      | nsfw | response_filter | true    |
    And the plugin manager replaces the response content with "[removed]"
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is 200
    And the response field "$.choices[0].message.content" is "[removed]"

  Scenario Outline: A request filter can reject the request
    Given the following plugins exist:
      | name     | type           | enabled |
      | validate | request_filter | true    |
    And the plugin manager rejects requests with status <status> and message "<message>"
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is <status>
    And the error code is "plugin_rejected"
    And the response field "$.error.message" is "<message>"
    And the plugin manager was not asked to filter the response

    Examples:
      | status | message                    |
      | 400    | Prompt is too long         |
      | 403    | Model is not allowed       |

  Scenario Outline: Invalid chat requests are rejected
    When I post the chat body '<body>'
    Then the response status is 400
    And the error code is "malformed_request"

    Examples:
      | body                                                                                         |
      | {"messages": [{"role": "user", "content": "Hi"}]}                                            |
      | {"model": "gpt-test", "messages": []}                                                        |
      | {"model": "gpt-test", "messages": [{"role": "user", "content": "Hi"}], "stream": true}       |
      | {"model": "gpt-test", "messages": [{"role": "user", "content": "Hi"}], "temperature": 2.5}   |
      | {"model": "gpt-test", "messages": [{"role": "user", "content": "Hi"}], "top_p": -0.1}        |
      | {"model": "gpt-test", "messages": [{"role": "user", "content": "Hi"}], "max_tokens": 0}      |
      | not a json                                                                                   |