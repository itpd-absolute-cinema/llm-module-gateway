Feature: Plugin metadata
  A plugin may declare a description and a JSON Schema for its options. The gateway reads them
  from the source without running it and returns them with the plugin, so the admin UI can
  build forms instead of a raw JSON editor.

  Background:
    Given I am authenticated as admin

  Scenario: Description and options schema declared in the plugin class are exposed
    When I upload the file "mask.py" of type "request_filter" with the source:
      """
      class Mask:
          description = "Masks phone numbers"
          options_schema = {"type": "object", "properties": {"mask": {"type": "string"}}}

          def apply(self, request):
              return request
      """
    Then the response status is 201
    And the response field "$.description" is "Masks phone numbers"
    And the response field "$.options_schema.type" is "object"
    And the response field "$.options_schema.properties.mask.type" is "string"
    When I get the plugin "mask"
    Then the response field "$.options_schema.properties.mask.type" is "string"
    When I list the plugins
    Then the response field "$[0].description" is "Masks phone numbers"

  Scenario: The first line of the class docstring is the fallback description
    When I upload the file "mask.py" of type "request_filter" with the source:
      """
      class Mask:
          '''Masks phone numbers.
          Longer text.'''

          def apply(self, request):
              return request
      """
    Then the response field "$.description" is "Masks phone numbers."
    And the response has no field "$.options_schema"

  Scenario: Metrics declared by the plugin are exposed
    When I upload the file "book_counter.py" of type "request_filter" with the source:
      """
      class BookCounter:
          metrics_spec = {
              "book_requests": {"type": "counter", "help": "Requests that mention a book"},
              "masked": {"type": "counter", "labels": {"kind": ["phone", "card"]}},
          }

          def apply(self, request):
              return request
      """
    Then the response status is 201
    And the response field "$.metrics.book_requests.type" is "counter"
    And the response field "$.metrics.masked.labels.kind[1]" is "card"
    When I get the plugin "book_counter"
    Then the response field "$.metrics.book_requests.help" is "Requests that mention a book"

  Scenario: A module-level METRICS is accepted too
    When I upload the file "m.py" of type "request_filter" with the source:
      """
      METRICS = {"hits": {"type": "counter"}}

      class M:
          def apply(self, request):
              return request
      """
    Then the response field "$.metrics.hits.type" is "counter"

  Scenario: A metrics declaration that is not a dict is rejected
    When I upload the file "bad_metrics.py" of type "request_filter" with the source:
      """
      class Bad:
          metrics_spec = ["book_requests"]

          def apply(self, request):
              return request
      """
    Then the response status is 400
    And the error code is "invalid_metrics_spec"
    And the plugin file "bad_metrics.py" does not exist in the plugins directory

  Scenario: Plugins without metadata have neither field
    When I upload the file "plain.py" with form fields:
      | type | request_filter |
    Then the response status is 201
    And the response has no field "$.description"
    And the response has no field "$.options_schema"
    And the response has no field "$.metrics"

  Scenario: An options schema that is not a dict is rejected and the plugin is not stored
    When I upload the file "bad.py" of type "request_filter" with the source:
      """
      class Bad:
          options_schema = ["not", "a", "schema"]

          def apply(self, request):
              return request
      """
    Then the response status is 400
    And the error code is "invalid_options_schema"
    And the plugin file "bad.py" does not exist in the plugins directory

  Scenario: The position in the chain is part of the plugin view
    Given the following plugins exist:
      | name | type           | enabled |
      | a    | request_filter | false   |
      | b    | request_filter | false   |
    When I list the plugins
    Then the response field "$[0].position" is "0"
    And the response field "$[1].position" is "1"
