Feature: Blocking users
  Admins block users by the `sub` claim. The block is stored in the gateway and checked on every
  request, so it works at once, without waiting for the user's token to expire.

  Background:
    Given I am authenticated as admin

  Scenario: A blocked user is rejected at once and let back in after unblocking
    When I block the user "user-1" with the reason "Abuse"
    Then the response status is 200
    And the response field "$.id" is "user-1"
    And the response field "$.blocked" is "true"
    And the response field "$.block.reason" is "Abuse"
    And the response field "$.block.blocked_by" is "admin-1"
    Given I am authenticated as a user without the admin role
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is 403
    And the error code is "user_blocked"
    And the plugin manager was not asked to filter the request
    Given I am authenticated as admin
    When I unblock the user "user-1"
    Then the response status is 200
    And the response field "$.blocked" is "false"
    Given I am authenticated as a user without the admin role
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is 200

  Scenario: Blocking one user does not affect others
    When I block the user "user-1" with the reason "Abuse"
    And I am authenticated as another user without the admin role
    And I ask the model "gpt-test" the question "Hello"
    Then the response status is 200

  Scenario: A blocked user cannot list models either
    When I block the user "user-1" with the reason "Abuse"
    And I am authenticated as a user without the admin role
    And I request GET "/models"
    Then the response status is 403
    And the error code is "user_blocked"

  Scenario: A blocked user cannot stream
    When I block the user "user-1" with the reason "Abuse"
    And I am authenticated as a user without the admin role
    And I stream the question "Hello" to the model "gpt-test"
    Then the response status is 403
    And the error code is "user_blocked"

  Scenario: Blocking twice is a conflict
    When I block the user "user-1" with the reason "Abuse"
    And I block the user "user-1" with the reason "Abuse again"
    Then the response status is 409
    And the error code is "user_already_blocked"

  Scenario: Unblocking a user who is not blocked is a conflict
    When I unblock the user "user-1"
    Then the response status is 409
    And the error code is "user_not_blocked"

  Scenario: An administrator cannot block themselves
    When I block the user "admin-1" with the reason "Oops"
    Then the response status is 409
    And the error code is "cannot_block_self"

  Scenario: A reason is required
    When I block the user "user-1" with the reason " "
    Then the response status is 400
    And the error code is "malformed_request"

  Scenario: Blocking an unknown user
    Given Keycloak does not know the user "ghost"
    When I block the user "ghost" with the reason "Abuse"
    Then the response status is 404
    And the error code is "user_not_found"

  Scenario: Blocking disables the account in Keycloak and unblocking enables it again
    When I block the user "user-1" with the reason "Abuse"
    Then Keycloak was asked to disable the user "user-1"
    And the response field "$.block.keycloak_disabled" is "true"
    When I unblock the user "user-1"
    Then Keycloak was asked to enable the user "user-1"

  Scenario: A Keycloak failure does not undo the block
    Given disabling users in Keycloak fails
    When I block the user "user-1" with the reason "Abuse"
    Then the response status is 200
    And the response field "$.block.keycloak_disabled" is "false"
    Given I am authenticated as a user without the admin role
    When I ask the model "gpt-test" the question "Hello"
    Then the response status is 403
    And the error code is "user_blocked"

  Scenario: An account that was already disabled in Keycloak is not enabled on unblock
    Given Keycloak knows the user "user-1" as already disabled
    When I block the user "user-1" with the reason "Abuse"
    And I unblock the user "user-1"
    Then Keycloak was not asked to change any user

  Scenario: Blocking works without Keycloak admin access
    Given Keycloak admin access is not configured
    When I block the user "user-1" with the reason "Abuse"
    Then the response status is 200
    And Keycloak was not asked to change any user

  Scenario: The user list shows block status and last activity
    Given Keycloak lists the users:
      | id      | username | email            |
      | admin-1 | admin    | admin@example.io |
      | user-2  | bob      | bob@example.io   |
    When I ask the model "gpt-test" the question "Hello"
    And I block the user "user-2" with the reason "Abuse"
    And I list the users
    Then the response status is 200
    And the response field "$.total" is "2"
    And the response field "$.items[0].id" is "admin-1"
    And the response field "$.items[0].blocked" is "false"
    And the response field "$.items[0].last_activity" is not null
    And the response field "$.items[1].id" is "user-2"
    And the response field "$.items[1].blocked" is "true"

  Scenario: The user list needs Keycloak admin access
    Given Keycloak admin access is not configured
    When I list the users
    Then the response status is 503
    And the error code is "identity_provider_not_configured"
