package ru.inno.itpd.gateway.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import ru.inno.itpd.gateway.core.client.pluginmanager.PluginManagerClient;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.cucumber.client.ApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

public class UsageSteps {
    @Autowired private ApiClient api;
    @Autowired private PluginManagerClient pluginManager;

    private String rememberedRequestId;

    // ---------- setup ----------

    @Given("the model {string} costs {double} per 1M input tokens and {double} per 1M output tokens")
    public void price(String model, double input, double output) {
        api.json(HttpMethod.PUT, "/admin/pricing",
                "{\"items\":[{\"model\":\"%s\",\"input_price_per_1m\":%s,\"output_price_per_1m\":%s,\"currency\":\"USD\"}]}"
                        .formatted(model, input, output));
        assertThat(api.status()).as("setting the price of %s", model).isEqualTo(200);
    }

    @Given("the plugin manager rejects responses with status {int} and message {string}")
    public void rejectsResponses(int status, String message) {
        doThrow(new GatewayException(HttpStatus.valueOf(status), "plugin_rejected", message))
                .when(pluginManager).filterResponse(any(), any(), anyList());
    }

    // ---------- calls ----------

    @When("I stream the question {string} to the model {string}")
    public void stream(String question, String model) {
        api.json(HttpMethod.POST, "/chat/completions", streamBody(model, question, false));
    }

    @When("I stream the question {string} to the model {string} asking for usage")
    public void streamWithUsage(String question, String model) {
        api.json(HttpMethod.POST, "/chat/completions", streamBody(model, question, true));
    }

    @When("I remember the request id of the response")
    public void remember() {
        rememberedRequestId = api.header("X-Request-ID");
        assertThat(rememberedRequestId).isNotBlank();
    }

    @When("I list the usage events")
    public void events() {
        api.request(HttpMethod.GET, "/admin/usage/events");
    }

    @When("I list the usage events with status {string}")
    public void eventsByStatus(String status) {
        api.request(HttpMethod.GET, "/admin/usage/events?status=" + status);
    }

    @When("I request the usage summary")
    public void summary() {
        api.request(HttpMethod.GET, "/admin/usage/summary");
    }

    @When("I replace the pricing with:")
    public void replacePricing(String body) {
        api.json(HttpMethod.PUT, "/admin/pricing", body);
    }

    // ---------- verification ----------

    @Then("the first usage event has the remembered request id")
    public void firstEventHasRequestId() {
        assertThat(api.<String>field("$.items[0].request_id")).isEqualTo(rememberedRequestId);
    }

    private static String streamBody(String model, String question, boolean includeUsage) {
        return "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"stream\":true%s}"
                .formatted(model, question, includeUsage ? ",\"stream_options\":{\"include_usage\":true}" : "");
    }
}
