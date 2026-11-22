package ru.inno.itpd.gateway.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient.KcUser;
import ru.inno.itpd.gateway.core.client.pluginmanager.PluginManagerClient;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.cucumber.client.ApiClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserSteps {
    @Autowired private ApiClient api;
    @Autowired private KeycloakAdminClient keycloak;
    @Autowired private PluginManagerClient pluginManager;

    // ---------- Keycloak behaviour ----------

    @Given("Keycloak lists the users:")
    public void keycloakLists(DataTable table) {
        List<KcUser> users = table.asMaps().stream()
                .map(r -> new KcUser(r.get("id"), r.get("username"), r.get("email"), null, null, true))
                .toList();
        when(keycloak.search(any(), anyInt(), anyInt())).thenReturn(users);
        when(keycloak.count(any())).thenReturn((long) users.size());
    }

    @Given("Keycloak does not know the user {string}")
    public void keycloakUnknown(String id) {
        when(keycloak.find(id)).thenReturn(Optional.empty());
    }

    @Given("Keycloak knows the user {string} as already disabled")
    public void keycloakDisabled(String id) {
        when(keycloak.find(id)).thenReturn(Optional.of(new KcUser(id, "name-" + id, null, null, null, false)));
    }

    @Given("disabling users in Keycloak fails")
    public void keycloakFails() {
        doThrow(new GatewayException(HttpStatus.SERVICE_UNAVAILABLE, "identity_provider_unavailable", "Could not connect to Keycloak"))
                .when(keycloak).setEnabled(anyString(), anyBoolean());
    }

    @Given("Keycloak admin access is not configured")
    public void keycloakNotConfigured() {
        when(keycloak.isConfigured()).thenReturn(false);
    }

    // ---------- calls ----------

    @When("I block the user {string} with the reason {string}")
    public void block(String id, String reason) {
        api.json(HttpMethod.POST, "/admin/users/" + id + "/block", "{\"reason\":\"%s\"}".formatted(reason));
    }

    @When("I unblock the user {string}")
    public void unblock(String id) {
        api.request(HttpMethod.POST, "/admin/users/" + id + "/unblock");
    }

    @When("I list the users")
    public void list() {
        api.request(HttpMethod.GET, "/admin/users");
    }

    // ---------- verification ----------

    @Then("Keycloak was asked to disable the user {string}")
    public void disabled(String id) {
        verify(keycloak).setEnabled(id, false);
    }

    @Then("Keycloak was asked to enable the user {string}")
    public void enabled(String id) {
        verify(keycloak).setEnabled(id, true);
    }

    @Then("Keycloak was not asked to change any user")
    public void untouched() {
        verify(keycloak, never()).setEnabled(anyString(), anyBoolean());
    }

    @Then("the plugin manager was not asked to filter the request")
    public void requestNotFiltered() {
        verify(pluginManager, never()).filterRequest(any(), anyList());
    }
}
