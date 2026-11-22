Feature: Creating users
  POST /admin/users creates an enabled user in Keycloak with a password and the realm role
  "chat-user". If the role cannot be assigned, the created user is deleted again.
  The password never reaches the audit log.

  Background:
    Given I am authenticated as admin

  Scenario: A user is created with the role chat-user
    Given Keycloak creates new users with the id "new-id-1"
    When I create a user with:
      """
      {"username": "Alice", "email": "Alice@Example.io", "first_name": "Alice", "last_name": "Liddell", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 201
    And the response field "$.id" is "new-id-1"
    And the response field "$.username" is "alice"
    And the response field "$.email" is "alice@example.io"
    And the response field "$.enabled" is "true"
    And the response field "$.blocked" is "false"
    And the response field "$.last_activity" is "null"
    And Keycloak was asked to create the user "alice" with email "alice@example.io" and a permanent password
    And Keycloak was asked to give the user "new-id-1" the realm role "chat-user"
    And Keycloak was not asked to delete any user

  Scenario: Only the username and the password are required
    Given Keycloak creates new users with the id "new-id-1"
    When I create a user with:
      """
      {"username": "bob", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 201
    And the response field "$.email" is "null"

  Scenario: A temporary password is passed to Keycloak
    Given Keycloak creates new users with the id "new-id-1"
    When I create a user with:
      """
      {"username": "bob", "password": "S3cret-Passw0rd!", "temporary_password": true}
      """
    Then the response status is 201
    And Keycloak was asked to create the user "bob" with a temporary password

  Scenario: A created user can be blocked right away
    Given Keycloak creates new users with the id "new-id-1"
    When I create a user with:
      """
      {"username": "bob", "password": "S3cret-Passw0rd!"}
      """
    And I block the user "new-id-1" with the reason "Test"
    Then the response status is 200
    And the response field "$.blocked" is "true"

  # ---------- Keycloak errors ----------

  Scenario: A duplicate username or email is a conflict
    Given Keycloak already has a user with the same username
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 409
    And the error code is "user_exists"
    And Keycloak was not asked to give anyone a role
    And Keycloak was not asked to delete any user

  Scenario: A password that breaks the realm policy is rejected with the reason from Keycloak
    Given the realm password policy rejects the password with the reason "Invalid password: minimum length 8."
    When I create a user with:
      """
      {"username": "alice", "password": "short"}
      """
    Then the response status is 422
    And the error code is "weak_password"
    And the response body contains "Invalid password: minimum length 8."
    And Keycloak was not asked to give anyone a role

  Scenario: Missing permissions of the service account are reported as 502
    Given Keycloak refuses to create users because the service account lacks permissions
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 502
    And the error code is "identity_provider_error"
    And the response body contains "lacks permissions"

  Scenario: Without Keycloak admin access users cannot be created
    Given Keycloak admin access is not configured
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 503
    And the error code is "identity_provider_not_configured"
    And Keycloak was not asked to create any user

  # ---------- rollback ----------

  Scenario: The user is deleted when the role cannot be assigned
    Given Keycloak creates new users with the id "new-id-1"
    And assigning the realm role in Keycloak fails
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 502
    And the error code is "identity_provider_error"
    And Keycloak was asked to delete the user "new-id-1"
    When I request GET "/admin/audit?action=user.create"
    Then the response field "$.total" is "0"

  Scenario: A failed rollback is reported together with the user id
    Given Keycloak creates new users with the id "new-id-1"
    And assigning the realm role in Keycloak fails
    And deleting users in Keycloak fails
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 502
    And the response body contains "new-id-1"
    And the response body contains "delete the user in Keycloak manually"

  # ---------- audit ----------

  Scenario: Creation is audited without the password
    Given Keycloak creates new users with the id "new-id-1"
    When I create a user with:
      """
      {"username": "alice", "email": "alice@example.io", "password": "S3cret-Passw0rd!", "temporary_password": true}
      """
    And I request GET "/admin/audit?action=user.create"
    Then the response status is 200
    And the response field "$.total" is "1"
    And the response field "$.items[0].action" is "user.create"
    And the response field "$.items[0].target_type" is "user"
    And the response field "$.items[0].target_id" is "new-id-1"
    And the response field "$.items[0].actor" is "admin-1"
    And the response field "$.items[0].before" is "null"
    And the response field "$.items[0].after.username" is "alice"
    And the response field "$.items[0].after.email" is "alice@example.io"
    And the response field "$.items[0].after.roles[0]" is "chat-user"
    And the response body does not contain "S3cret-Passw0rd!"
    When I request GET "/admin/audit"
    Then the response body does not contain "S3cret"
    And the response body does not contain "password"

  # ---------- validation ----------

  Scenario Outline: An invalid body is rejected before Keycloak is called
    When I create a user with:
      """
      <body>
      """
    Then the response status is 400
    And the error code is "malformed_request"
    And Keycloak was not asked to create any user

    Examples:
      | body                                                                       |
      | {"password": "S3cret-Passw0rd!"}                                           |
      | {"username": "", "password": "S3cret-Passw0rd!"}                           |
      | {"username": "   ", "password": "S3cret-Passw0rd!"}                        |
      | {"username": "alice"}                                                      |
      | {"username": "alice", "password": ""}                                      |
      | {"username": "alice", "password": "S3cret-Passw0rd!", "email": "not-mail"} |
      | {"username": "alice", "password": "S3cret-Passw0rd!", "temporary_password": "yes"} |
      | not json                                                                   |

  Scenario: A username longer than 100 characters is rejected
    When I create a user with a username of 101 characters
    Then the response status is 400
    And the error code is "malformed_request"
    And Keycloak was not asked to create any user

  Scenario: A username of exactly 100 characters is accepted
    Given Keycloak creates new users with the id "new-id-1"
    When I create a user with a username of 100 characters
    Then the response status is 201

  # ---------- access ----------

  Scenario: Creating users needs a token
    Given I am not authenticated
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 401
    And the error code is "unauthorized"
    And Keycloak was not asked to create any user

  Scenario: Creating users needs the gateway-admin role
    Given I am authenticated as a user without the admin role
    When I create a user with:
      """
      {"username": "alice", "password": "S3cret-Passw0rd!"}
      """
    Then the response status is 403
    And the error code is "forbidden"
    And Keycloak was not asked to create any user
