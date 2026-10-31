package ru.inno.itpd.gateway.cucumber;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.inno.itpd.gateway.ChatCompletionResponse;
import ru.inno.itpd.gateway.ChatRequest;
import ru.inno.itpd.gateway.GatewayException;
import ru.inno.itpd.gateway.PluginDtos.PluginInfo;
import ru.inno.itpd.gateway.PluginManagerClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ChatSteps {
    private static final String CHAT_PATH = "/chat/completions";

    @Autowired private ApiClient api;
    @Autowired private PluginManagerClient pluginManager;

    // ---------- plugin manager behaviour ----------

    @Given("the plugin manager masks phone numbers in requests")
    public void masksPhones() {
        doAnswer(inv -> {
            ChatRequest request = inv.getArgument(0);
            request.getMessages().forEach(m ->
                    m.setContent(m.getContent().replaceAll("\\+?\\d[\\d\\s-]{8,}\\d", "[PHONE_MASK]")));
            return request;
        }).when(pluginManager).filterRequest(any(), anyList());
    }

    @Given("the plugin manager routes requests to the model {string}")
    public void routesTo(String model) {
        doAnswer(inv -> {
            ChatRequest request = inv.getArgument(0);
            request.setModel(model);
            return request;
        }).when(pluginManager).filterRequest(any(), anyList());
    }

    @Given("the plugin manager replaces the response content with {string}")
    public void replacesContent(String text) {
        doAnswer(inv -> {
            ChatCompletionResponse response = inv.getArgument(1);
            response.getChoices().forEach(c -> c.getMessage().setContent(text));
            return response;
        }).when(pluginManager).filterResponse(any(), any(), anyList());
    }

    @Given("the plugin manager rejects requests with status {int} and message {string}")
    public void rejects(int status, String message) {
        doThrow(new GatewayException(HttpStatus.valueOf(status), "plugin_rejected", message))
                .when(pluginManager).filterRequest(any(), anyList());
    }

    // ---------- calls ----------

    @When("I ask the model {string} the question {string}")
    public void ask(String model, String question) {
        String body = "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}"
                .formatted(model, question);
        api.json(HttpMethod.POST, CHAT_PATH, body);
    }

    @When("I post the chat body {string}")
    public void postBody(String body) {
        api.json(HttpMethod.POST, CHAT_PATH, body);
    }

    // ---------- what the plugin manager received ----------

    @Then("the request filters received by the plugin manager are, in order:")
    public void requestFilters(DataTable expected) {
        assertThat(capturedRequestFilters()).extracting(PluginInfo::name)
                .containsExactlyElementsOf(expected.asList());
    }

    @Then("the response filters received by the plugin manager are, in order:")
    public void responseFilters(DataTable expected) {
        assertThat(capturedResponseFilters()).extracting(PluginInfo::name)
                .containsExactlyElementsOf(expected.asList());
    }

    @Then("the plugin manager received no request filters")
    public void noRequestFilters() {
        assertThat(capturedRequestFilters()).isEmpty();
    }

    @Then("the plugin manager received no response filters")
    public void noResponseFilters() {
        assertThat(capturedResponseFilters()).isEmpty();
    }

    @Then("the options passed to the plugin manager for the request filter {string} contain {string} equal to {string}")
    public void optionsContain(String plugin, String key, String value) {
        PluginInfo info = capturedRequestFilters().stream()
                .filter(p -> p.name().equals(plugin)).findFirst().orElseThrow();
        assertThat(String.valueOf(info.options().get(key))).isEqualTo(value);
    }

    @Then("the plugin manager was not asked to filter the response")
    public void responseNotFiltered() {
        verify(pluginManager, never()).filterResponse(any(), any(), anyList());
    }

    @SuppressWarnings("unchecked")
    private List<PluginInfo> capturedRequestFilters() {
        ArgumentCaptor<List<PluginInfo>> captor =
                ArgumentCaptor.forClass((Class<List<PluginInfo>>) (Class<?>) List.class);
        verify(pluginManager).filterRequest(any(), captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<PluginInfo> capturedResponseFilters() {
        ArgumentCaptor<List<PluginInfo>> captor =
                ArgumentCaptor.forClass((Class<List<PluginInfo>>) (Class<?>) List.class);
        verify(pluginManager).filterResponse(any(), any(), captor.capture());
        return captor.getValue();
    }
}