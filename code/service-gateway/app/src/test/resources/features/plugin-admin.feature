Feature: Plugin administration API
  Admins upload, configure, order, enable and delete gateway plugins.
  A plugin is either a request_filter or a response_filter.

  Background:
    Given I am authenticated as admin

  # ---------- upload ----------

  Scenario: Uploading a plugin creates it disabled
    When I upload the file "mask_phones.py" with form fields:
      | type    | request_filter       |
      | options | {"replacement": "*"} |
    Then the response status is 201
    And the response header "X-Request-ID" is present
    And the response field "$.name" is "mask_phones"
    And the response field "$.type" is "request_filter"
    And the response field "$.enabled" is "false"
    And the response field "$.options.replacement" is "*"
    And the plugin file "mask_phones.py" exists in the plugins directory

  Scenario: Options default to an empty object
    When I upload the file "nsfw.py" with form fields:
      | type | response_filter |
    Then the response status is 201
    And the response field "$.type" is "response_filter"
    And the response field "$.options" is "{}"

  Scenario Outline: Uploads that break the rules are rejected
    When I upload the file "<filename>" with form fields:
      | type    | <type>    |
      | options | <options> |
    Then the response status is <status>
    And the error code is "<code>"

    Examples:
      | filename    | type           | options     | status | code                      |
      | plugin.txt  | request_filter | {}          | 400    | invalid_filename          |
      | bad-name.py | request_filter | {}          | 400    | invalid_plugin_name       |
      | plugin.py   | postprocessor  | {}          | 400    | invalid_plugin_type       |
      | plugin.py   | request_filter | [1, 2]      | 400    | invalid_options           |
      | plugin.py   | request_filter | {"a": null} | 422    | options_not_representable |

  Scenario: The plugin type is required
    When I upload the file "plugin.py" with form fields:
      | options | {} |
    Then the response status is 400
    And the error code is "malformed_request"

  Scenario: A file with invalid Python syntax is rejected and not stored
    When I upload the file "broken.py" of type "request_filter" with invalid Python source
    Then the response status is 400
    And the error code is "invalid_python_source"
    And the plugin file "broken.py" does not exist in the plugins directory

  Scenario: A source larger than 1 MiB is rejected
    When I upload the file "huge.py" of type "request_filter" with a source of 1200000 bytes
    Then the response status is 413
    And the error code is "plugin_too_large"

  Scenario: A plugin cannot be uploaded twice
    Given the following plugins exist:
      | name | type           |
      | mask | request_filter |
    When I upload the file "mask.py" with form fields:
      | type | response_filter |
    Then the response status is 409
    And the error code is "plugin_exists"

  # ---------- ordering ----------

  Scenario: New plugins are appended to the end of the chain
    Given the following plugins exist:
      | name | type            |
      | a    | request_filter  |
      | b    | response_filter |
      | c    | request_filter  |
    When I list the plugins
    Then the plugin names in the response are, in order:
      | a |
      | b |
      | c |

  Scenario: A new plugin can be inserted before an existing one
    Given the following plugins exist:
      | name | type           |
      | a    | request_filter |
      | b    | request_filter |
    When I upload the file "c.py" with form fields:
      | type   | request_filter |
      | before | a              |
    Then the response status is 201
    When I list the plugins
    Then the plugin names in the response are, in order:
      | c |
      | a |
      | b |

  Scenario: A new plugin can be inserted after an existing one
    Given the following plugins exist:
      | name | type           |
      | a    | request_filter |
      | b    | request_filter |
    When I upload the file "c.py" with form fields:
      | type  | request_filter |
      | after | a              |
    Then the response status is 201
    When I list the plugins
    Then the plugin names in the response are, in order:
      | a |
      | c |
      | b |

  Scenario: before and after are mutually exclusive
    Given the following plugins exist:
      | name | type           |
      | a    | request_filter |
      | b    | request_filter |
    When I upload the file "c.py" with form fields:
      | type   | request_filter |
      | before | a              |
      | after  | b              |
    Then the response status is 400
    And the error code is "invalid_ordering"

  Scenario: The ordering target must exist
    When I upload the file "c.py" with form fields:
      | type   | request_filter |
      | before | ghost          |
    Then the response status is 404
    And the error code is "ordering_target_not_found"

  # ---------- read ----------

  Scenario: Listing plugins shows type and state
    Given the following plugins exist:
      | name | type            | enabled |
      | a    | request_filter  | true    |
      | b    | response_filter | false   |
    When I list the plugins
    Then the response status is 200
    And the response field "$[0].name" is "a"
    And the response field "$[0].enabled" is "true"
    And the response field "$[1].type" is "response_filter"
    And the response field "$[1].enabled" is "false"

  Scenario: Reading one plugin
    Given the following plugins exist:
      | name | type           | options        |
      | mask | request_filter | {"depth": 2}   |
    When I get the plugin "mask"
    Then the response status is 200
    And the response field "$.name" is "mask"
    And the response field "$.options.depth" is "2"

  Scenario: Reading an unknown plugin
    When I get the plugin "ghost"
    Then the response status is 404
    And the error code is "plugin_not_found"

  Scenario: Reading a plugin with an invalid name
    When I get the plugin "bad-name"
    Then the response status is 400
    And the error code is "invalid_plugin_name"

  # ---------- settings ----------

  Scenario: Updated settings replace the old ones and are visible on read
    Given the following plugins exist:
      | name | type           | options              |
      | mask | request_filter | {"replacement": "*"} |
    When I update the plugin "mask" with:
      """
      {"options": {"replacement": "#", "countries": ["RU", "FI"]}}
      """
    Then the response status is 200
    And the response field "$.options.replacement" is "#"
    When I get the plugin "mask"
    Then the response field "$.options.replacement" is "#"
    And the response field "$.options.countries[1]" is "FI"
    And the response field "$.type" is "request_filter"

  Scenario Outline: Invalid settings are rejected
    Given the following plugins exist:
      | name | type           |
      | mask | request_filter |
    When I update the plugin "mask" with:
      """
      <body>
      """
    Then the response status is <status>
    And the error code is "<code>"

    Examples:
      | body                     | status | code                      |
      | {}                       | 400    | malformed_request         |
      | {"options": {"x": null}} | 422    | options_not_representable |

  Scenario: Updating settings of an unknown plugin
    When I update the plugin "ghost" with:
      """
      {"options": {}}
      """
    Then the response status is 404
    And the error code is "plugin_not_found"

  # ---------- status and position ----------

  Scenario: Enabling and disabling a plugin
    Given the following plugins exist:
      | name | type           |
      | mask | request_filter |
    When I patch the plugin "mask" with:
      """
      {"enabled": true}
      """
    Then the response status is 200
    And the response field "$.enabled" is "true"
    When I patch the plugin "mask" with:
      """
      {"enabled": false}
      """
    Then the response field "$.enabled" is "false"

  Scenario: Moving a plugin within the chain
    Given the following plugins exist:
      | name | type           |
      | a    | request_filter |
      | b    | request_filter |
      | c    | request_filter |
    When I patch the plugin "a" with:
      """
      {"after": "b"}
      """
    Then the response status is 200
    When I list the plugins
    Then the plugin names in the response are, in order:
      | b |
      | a |
      | c |

  Scenario: Moving a plugin relative to an unknown plugin
    Given the following plugins exist:
      | name | type           |
      | a    | request_filter |
    When I patch the plugin "a" with:
      """
      {"before": "ghost"}
      """
    Then the response status is 404
    And the error code is "ordering_target_not_found"

  # ---------- delete ----------

  Scenario: Deleting a disabled plugin removes it and its file
    Given the following plugins exist:
      | name | type           |
      | mask | request_filter |
    When I delete the plugin "mask"
    Then the response status is 204
    And the plugin file "mask.py" does not exist in the plugins directory
    When I get the plugin "mask"
    Then the response status is 404

  Scenario: An enabled plugin cannot be deleted
    Given the following plugins exist:
      | name | type           | enabled |
      | mask | request_filter | true    |
    When I delete the plugin "mask"
    Then the response status is 409
    And the error code is "plugin_enabled"
    And the plugin file "mask.py" exists in the plugins directory

  Scenario: Deleting an unknown plugin
    When I delete the plugin "ghost"
    Then the response status is 404
    And the error code is "plugin_not_found"

  # ---------- security ----------

  Scenario Outline: Admin endpoints require a bearer token
    Given I am not authenticated
    When I request <method> "<path>"
    Then the response status is 401
    And the error code is "unauthorized"
    And the response header "WWW-Authenticate" is "Bearer"
    And the response header "X-Request-ID" is present

    Examples:
      | method | path               |
      | GET    | /admin/plugins     |
      | GET    | /admin/plugins/any |
      | DELETE | /admin/plugins/any |

  Scenario: A wrong token is rejected
    Given I authenticate with the token "wrong-key"
    When I list the plugins
    Then the response status is 401