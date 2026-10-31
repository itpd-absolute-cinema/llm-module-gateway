package ru.inno.itpd.gateway.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    @Autowired private ApiClient api;

    @Given("I am authenticated as admin")
    public void authenticatedAsAdmin() {
        api.authenticate(CucumberSpringConfig.ADMIN_KEY);
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
}