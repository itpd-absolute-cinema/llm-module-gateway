Feature: Admin audit log
  Changes to plugins, users and prices are logged with the actor and the values before and after.

  Background:
    Given I am authenticated as admin

  Scenario: Actions are recorded, newest first
    When I upload the file "mask.py" with form fields:
      | type    | request_filter |
      | options | {"a": 1}       |
    And I patch the plugin "mask" with:
      """
      {"enabled": true}
      """
    And I block the user "user-1" with the reason "Abuse"
    And I replace the pricing with:
      """
      {"items": [{"model": "a", "input_price_per_1m": 1, "output_price_per_1m": 2, "currency": "USD"}]}
      """
    And I request GET "/admin/audit"
    Then the response status is 200
    And the response field "$.total" is "4"
    And the response field "$.items[0].action" is "pricing.update"
    And the response field "$.items[0].after.items[0].model" is "a"
    And the response field "$.items[1].action" is "user.block"
    And the response field "$.items[1].target_id" is "user-1"
    And the response field "$.items[1].before.blocked" is "false"
    And the response field "$.items[1].after.reason" is "Abuse"
    And the response field "$.items[2].action" is "plugin.status"
    And the response field "$.items[2].before.enabled" is "false"
    And the response field "$.items[2].after.enabled" is "true"
    And the response field "$.items[3].action" is "plugin.create"
    And the response field "$.items[3].actor" is "admin-1"
    And the response field "$.items[3].actor_username" is "admin"
    And the response field "$.items[3].request_id" is not null

  Scenario: Updating settings and deleting a plugin are recorded
    Given the following plugins exist:
      | name | type           | enabled |
      | mask | request_filter | false   |
    And I update the plugin "mask" with:
      """
      {"options": {"mask": "*"}}
      """
    And I delete the plugin "mask"
    When I request GET "/admin/audit?action=plugin.update"
    Then the response field "$.total" is "1"
    And the response field "$.items[0].after.options.mask" is "*"
    When I request GET "/admin/audit?action=plugin.delete"
    Then the response field "$.total" is "1"
    And the response field "$.items[0].before.name" is "mask"

  Scenario: Reading does not create entries and the log can be filtered by actor
    Given the following plugins exist:
      | name | type           | enabled |
      | mask | request_filter | false   |
    When I list the plugins
    And I request GET "/admin/audit?actor=nobody"
    Then the response field "$.total" is "0"
    When I request GET "/admin/audit?actor=admin"
    Then the response field "$.total" is "1"
