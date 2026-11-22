package ru.inno.itpd.gateway.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient.NewUser;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.cucumber.client.ApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserCreationSteps {
    @Autowired private ApiClient api;
    @Autowired private KeycloakAdminClient keycloak;

    // ---------- поведение Keycloak ----------

    @Given("Keycloak creates new users with the id {string}")
    public void createsWithId(String id) {
        when(keycloak.createUser(any())).thenReturn(id);
    }

    @Given("Keycloak already has a user with the same username")
    public void duplicate() {
        doThrow(new GatewayException(HttpStatus.CONFLICT, "user_exists", "User exists with same username"))
                .when(keycloak).createUser(any());
    }

    @Given("the realm password policy rejects the password with the reason {string}")
    public void weakPassword(String reason) {
        doThrow(new GatewayException(HttpStatus.UNPROCESSABLE_ENTITY, "weak_password",
                "Password rejected by the realm policy: " + reason))
                .when(keycloak).createUser(any());
    }

    @Given("Keycloak refuses to create users because the service account lacks permissions")
    public void noPermissions() {
        doThrow(new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                "Keycloak denied access while creating the user (status 403): the gateway service account lacks permissions"))
                .when(keycloak).createUser(any());
    }

    @Given("assigning the realm role in Keycloak fails")
    public void roleFails() {
        doThrow(new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                "Realm role 'chat-user' does not exist in Keycloak"))
                .when(keycloak).assignRealmRole(anyString(), anyString());
    }

    @Given("deleting users in Keycloak fails")
    public void deleteFails() {
        doThrow(new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error", "Keycloak responded with status 500"))
                .when(keycloak).deleteUser(anyString());
    }

    // ---------- вызовы ----------

    @When("I create a user with:")
    public void create(String body) {
        api.json(HttpMethod.POST, "/admin/users", body);
    }

    @When("I create a user with a username of {int} characters")
    public void createWithLongUsername(int length) {
        api.json(HttpMethod.POST, "/admin/users",
                "{\"username\": \"%s\", \"password\": \"S3cret-Passw0rd!\"}".formatted("a".repeat(length)));
    }

    // ---------- проверки ----------

    @Then("Keycloak was asked to create the user {string} with email {string} and a permanent password")
    public void createdPermanent(String username, String email) {
        NewUser user = captureCreated();
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.password()).isEqualTo("S3cret-Passw0rd!");
        assertThat(user.temporaryPassword()).isFalse();
    }

    @Then("Keycloak was asked to create the user {string} with a temporary password")
    public void createdTemporary(String username) {
        NewUser user = captureCreated();
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.temporaryPassword()).isTrue();
    }

    @Then("Keycloak was asked to give the user {string} the realm role {string}")
    public void roleAssigned(String id, String role) {
        verify(keycloak).assignRealmRole(id, role);
    }

    @Then("Keycloak was asked to delete the user {string}")
    public void deleted(String id) {
        verify(keycloak).deleteUser(id);
    }

    @Then("Keycloak was not asked to delete any user")
    public void notDeleted() {
        verify(keycloak, never()).deleteUser(anyString());
    }

    @Then("Keycloak was not asked to create any user")
    public void notCreated() {
        verify(keycloak, never()).createUser(any());
    }

    @Then("Keycloak was not asked to give anyone a role")
    public void noRole() {
        verify(keycloak, never()).assignRealmRole(anyString(), anyString());
    }

    private NewUser captureCreated() {
        ArgumentCaptor<NewUser> captor = ArgumentCaptor.forClass(NewUser.class);
        verify(keycloak).createUser(captor.capture());
        return captor.getValue();
    }
}
