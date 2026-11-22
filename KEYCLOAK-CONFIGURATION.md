Понял. Вот обновлённый `KEYCLOAK_SETUP.md`: только настройка через Keycloak Admin Console, без Docker, команд, JSON и программной конфигурации.

# Keycloak Configuration Guide

This document describes how to configure Keycloak for the Gateway application using the Keycloak Administration Console.

## 1. Create a Realm

1. Open the Keycloak Administration Console.
2. Log in as an administrator.
3. Open the realm selector in the top-left corner.
4. Click **Create realm**.
5. Enter the following realm name:

   `gateway`
6. Click **Create**.

The `gateway` realm will be used to manage users and authenticate requests for the Gateway application.

## 2. Create an OpenID Connect Client

1. Select the `gateway` realm.
2. Open **Clients** in the navigation menu.
3. Click **Create client**.
4. Configure the client:
   - **Client type:** OpenID Connect
   - **Client ID:** `gateway`
5. Click **Next**.
6. Configure the following capability settings:

| Setting                | Value |
| ---------------------- | ----- |
| Client authentication  | Off   |
| Authorization          | Off   |
| Standard flow          | On    |
| Direct access grants   | On    |
| Service accounts roles | Off   |

7. Save the client.

The client is now configured as a public OpenID Connect client.

## 3. Create a Test User

1. Open **Users** in the navigation menu.
2. Click **Add user**.
3. Configure the user:

| Field          | Value              |
| -------------- | ------------------ |
| Username       | `test`             |
| Email          | `test@example.com` |
| First name     | `Test`             |
| Last name      | `User`             |
| Email verified | On                 |
| User enabled   | On                 |

4. Click **Create**.

**Important:** First name and Last name must be populated. Leaving these fields empty caused authentication to fail with an "Account is not fully set up" error.

## 4. Configure the User Password

1. Open the newly created `test` user.
2. Navigate to the **Credentials** tab.
3. Click **Set password**.
4. Enter the password:

   `12345`
5. Set **Temporary** to **Off**.
6. Save the password.

The user is now ready for authentication.

## 5. Verify Realm Login Settings

1. Open **Realm settings**.
2. Navigate to the **Login** tab.
3. Review the email verification settings.
4. For a local development environment, ensure that email verification does not prevent the test user from signing in.

If email verification is enabled, the user's email address must be verified before authentication can succeed.

## 6. Authentication Configuration Summary

The following configuration should be present:

| Component        | Setting               | Value              |
| ---------------- | --------------------- | ------------------ |
| Realm            | Name                  | `gateway`          |
| Client           | Client ID             | `gateway`          |
| Client           | Client type           | OpenID Connect     |
| Client           | Client authentication | Off                |
| Client           | Standard flow         | On                 |
| Client           | Direct access grants  | On                 |
| User             | Username              | `test`             |
| User             | First name            | `Test`             |
| User             | Last name             | `User`             |
| User             | Email                 | `test@example.com` |
| User             | Enabled               | On                 |
| User credentials | Temporary password    | Off                |

## 7. Important Notes

- The `gateway` realm manages authentication for the Gateway application.
- The `gateway` client represents the application in Keycloak.
- Users must have valid credentials and complete any required account setup actions before authentication.
- First name and Last name should be populated to avoid the account setup error encountered during testing.
