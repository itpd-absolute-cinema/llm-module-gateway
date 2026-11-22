package ru.inno.itpd.gateway.cucumber.steps;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import ru.inno.itpd.gateway.cucumber.client.ApiClient;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PluginMetadataSteps {
    @Autowired private ApiClient api;

    @When("I upload the file {string} of type {string} with the source:")
    public void uploadSource(String filename, String type, String source) {
        api.upload(filename, source.getBytes(UTF_8), Map.of("type", type));
    }

    @When("I send a CORS preflight for {string} {string} from {string}")
    public void preflight(String method, String path, String origin) {
        api.request(org.springframework.http.HttpMethod.OPTIONS, path, Map.of(
                "Origin", origin,
                "Access-Control-Request-Method", method,
                "Access-Control-Request-Headers", "authorization,content-type"));
    }

    @When("I list the plugins from the origin {string}")
    public void listFromOrigin(String origin) {
        api.request(org.springframework.http.HttpMethod.GET, "/admin/plugins", Map.of("Origin", origin));
    }
}
