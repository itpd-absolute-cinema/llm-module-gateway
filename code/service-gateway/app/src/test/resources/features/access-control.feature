Feature: Access control
  /admin/** needs the realm role gateway-admin: no token or a bad token is 401, a valid token
  without the role is 403. /chat/** and /models need any valid token.

  Scenario Outline: Admin endpoints require a bearer token
    Given I am not authenticated
    When I request <method> "<path>"
    Then the response status is 401
    And the error code is "unauthorized"

    Examples:
      | method | path                           |
      | GET    | /admin/users                   |
      | GET    | /admin/users/user-1            |
      | POST   | /admin/users/user-1/block      |
      | POST   | /admin/users/user-1/unblock    |
      | GET    | /admin/usage/summary           |
      | GET    | /admin/usage/timeseries        |
      | GET    | /admin/usage/events            |
      | GET    | /admin/usage/export.csv        |
      | GET    | /admin/pricing                 |
      | PUT    | /admin/pricing                 |
      | GET    | /admin/models                  |
      | GET    | /admin/audit                   |

  Scenario Outline: A valid token without the gateway-admin role is forbidden
    Given I am authenticated as a user without the admin role
    When I request <method> "<path>"
    Then the response status is 403
    And the error code is "forbidden"

    Examples:
      | method | path                           |
      | GET    | /admin/users                   |
      | GET    | /admin/users/user-1            |
      | POST   | /admin/users/user-1/block      |
      | POST   | /admin/users/user-1/unblock    |
      | GET    | /admin/usage/summary           |
      | GET    | /admin/usage/timeseries        |
      | GET    | /admin/usage/events            |
      | GET    | /admin/usage/export.csv        |
      | GET    | /admin/pricing                 |
      | PUT    | /admin/pricing                 |
      | GET    | /admin/models                  |
      | GET    | /admin/audit                   |

  Scenario Outline: The admin role opens the admin endpoints
    Given I am authenticated as admin
    When I request GET "<path>"
    Then the response status is 200

    Examples:
      | path                    |
      | /admin/users            |
      | /admin/users/user-1     |
      | /admin/usage/summary    |
      | /admin/usage/events     |
      | /admin/usage/export.csv |
      | /admin/pricing          |
      | /admin/models           |
      | /admin/audit            |

  Scenario: A wrong token is rejected on the admin API
    Given I authenticate with the token "wrong-key"
    When I request GET "/admin/pricing"
    Then the response status is 401

  Scenario Outline: Client endpoints require a token
    Given I am not authenticated
    When I request <method> "<path>"
    Then the response status is 401
    And the error code is "unauthorized"
    And the response header "WWW-Authenticate" is "Bearer"

    Examples:
      | method | path              |
      | GET    | /models           |
      | POST   | /chat/completions |

  Scenario: A wrong token is rejected on the client API
    Given I authenticate with the token "wrong-key"
    When I request GET "/models"
    Then the response status is 401

  Scenario: Client endpoints do not need the admin role
    Given I am authenticated as a user without the admin role
    When I request GET "/models"
    Then the response status is 200

  # ---------- CORS ----------

  Scenario: A preflight from the admin origin is allowed
    Given I am not authenticated
    When I send a CORS preflight for "PATCH" "/admin/plugins/mask" from "http://admin.test"
    Then the response status is 200
    And the response header "Access-Control-Allow-Origin" is "http://admin.test"

  Scenario: A preflight from another origin is refused
    Given I am not authenticated
    When I send a CORS preflight for "PATCH" "/admin/plugins/mask" from "http://evil.test"
    Then the response status is 403

  Scenario: Responses to the admin origin carry the CORS header
    Given I am authenticated as admin
    When I list the plugins from the origin "http://admin.test"
    Then the response status is 200
    And the response header "Access-Control-Allow-Origin" is "http://admin.test"
