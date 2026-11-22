package ru.inno.itpd.gateway.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import ru.inno.itpd.gateway.cucumber.config.TestSecurityConfiguration;
import ru.inno.itpd.gateway.cucumber.client.ApiClient;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    @Autowired private ApiClient api;

    @Given("I am authenticated as admin")
    public void authenticatedAsAdmin() {
        api.authenticate(TestSecurityConfiguration.ADMIN_TOKEN);
    }

    @Given("I am authenticated as a user without the admin role")
    public void authenticatedAsPlainUser() {
        api.authenticate(TestSecurityConfiguration.USER_TOKEN);
    }

    @Given("I am authenticated as another user without the admin role")
    public void authenticatedAsOtherUser() {
        api.authenticate(TestSecurityConfiguration.OTHER_USER_TOKEN);
    }

    @Given("I am not authenticated")
    public void notAuthenticated() {
        api.authenticate(null);
    }

    @Given("I authenticate with the token {string}")
    public void authenticateWith(String token) {
        api.authenticate(token);
    }

    @Then("the response status is {int}")
    public void statusIs(int status) {
        assertThat(api.status()).isEqualTo(status);
    }

    @Then("the error code is {string}")
    public void errorCodeIs(String code) {
        assertThat(api.<String>field("$.error.code")).isEqualTo(code);
    }

    @Then("the response header {string} is present")
    public void headerPresent(String name) {
        assertThat(api.header(name)).isNotBlank();
    }

    @Then("the response header {string} is {string}")
    public void headerIs(String name, String value) {
        assertThat(api.header(name)).isEqualTo(value);
    }

    @Then("the response field {string} is {string}")
    public void fieldIs(String jsonPath, String expected) {
        assertThat(String.valueOf(api.<Object>field(jsonPath))).isEqualTo(expected);
    }

    @Then("the response field {string} is not null")
    public void fieldNotNull(String jsonPath) {
        assertThat(api.<Object>field(jsonPath)).isNotNull();
    }

    @Then("the response has no field {string}")
    public void noField(String jsonPath) {
        try {
            Object value = api.<Object>field(jsonPath);
            assertThat(value).as("field %s", jsonPath).isNull();
        } catch (com.jayway.jsonpath.PathNotFoundException expected) {
            // the field is absent, which is what we want
        }
    }

    @Then("the response field {string} is greater than {int}")
    public void fieldGreaterThan(String jsonPath, int min) {
        assertThat(api.<Number>field(jsonPath).doubleValue()).isGreaterThan(min);
    }

    @Then("the response field {string} is approximately {double}")
    public void fieldApproximately(String jsonPath, double expected) {
        assertThat(api.<Number>field(jsonPath).doubleValue()).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-4));
    }

    @Then("the response header {string} starts with {string}")
    public void headerStartsWith(String name, String prefix) {
        assertThat(api.header(name)).startsWith(prefix);
    }

    @Then("the response body contains {string}")
    public void bodyContains(String text) {
        assertThat(api.body()).contains(text);
    }

    @Then("the response body does not contain {string}")
    public void bodyDoesNotContain(String text) {
        assertThat(api.body()).doesNotContain(text);
    }
}
