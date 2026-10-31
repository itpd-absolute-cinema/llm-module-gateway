package ru.inno.itpd.gateway.cucumber;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class PluginAdminSteps {
    private static final byte[] VALID_SOURCE = "def process(data, options):\n    return data\n".getBytes(UTF_8);

    @Autowired private ApiClient api;

    /** Uploads plugins in row order (so the chain order equals the row order) and enables the marked ones. */
    @Given("the following plugins exist:")
    public void pluginsExist(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            String name = row.get("name");
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("type", row.get("type"));
            fields.put("options", row.getOrDefault("options", "{}"));
            api.upload(name + ".py", VALID_SOURCE, fields);
            assertThat(api.status()).as("upload of %s", name).isEqualTo(201);

            if (Boolean.parseBoolean(row.getOrDefault("enabled", "false"))) {
                api.json(HttpMethod.PATCH, "/admin/plugins/" + name, "{\"enabled\": true}");
                assertThat(api.status()).as("enabling %s", name).isEqualTo(200);
            }
        }
    }

    @When("I upload the file {string} with form fields:")
    public void uploadWithFields(String filename, DataTable fields) {
        api.upload(filename, VALID_SOURCE, fields.asMap());
    }

    @When("I upload the file {string} of type {string} with invalid Python source")
    public void uploadInvalidPython(String filename, String type) {
        api.upload(filename, "def broken(:\n".getBytes(UTF_8), Map.of("type", type));
    }

    @When("I upload the file {string} of type {string} with a source of {int} bytes")
    public void uploadBig(String filename, String type, int size) {
        byte[] content = ("#".repeat(size - 1) + "\n").getBytes(UTF_8);   // a valid Python comment
        api.upload(filename, content, Map.of("type", type));
    }

    @When("I list the plugins")
    public void list() {
        api.request(HttpMethod.GET, "/admin/plugins");
    }

    @When("I get the plugin {string}")
    public void get(String name) {
        api.request(HttpMethod.GET, "/admin/plugins/" + name);
    }

    @When("I update the plugin {string} with:")
    public void update(String name, String body) {
        api.json(HttpMethod.PUT, "/admin/plugins/" + name, body);
    }

    @When("I patch the plugin {string} with:")
    public void patch(String name, String body) {
        api.json(HttpMethod.PATCH, "/admin/plugins/" + name, body);
    }

    @When("I delete the plugin {string}")
    public void delete(String name) {
        api.request(HttpMethod.DELETE, "/admin/plugins/" + name);
    }

    @When("I request {word} {string}")
    public void request(String method, String path) {
        api.request(HttpMethod.valueOf(method), path);
    }

    @Then("the plugin names in the response are, in order:")
    public void namesInOrder(DataTable expected) {
        List<String> names = api.field("$[*].name");
        assertThat(names).containsExactlyElementsOf(expected.asList());
    }

    @Then("the plugin file {string} exists in the plugins directory")
    public void fileExists(String filename) {
        assertThat(Files.exists(CucumberSpringConfig.PLUGINS_DIR.resolve(filename))).isTrue();
    }

    @Then("the plugin file {string} does not exist in the plugins directory")
    public void fileDoesNotExist(String filename) {
        assertThat(Files.exists(CucumberSpringConfig.PLUGINS_DIR.resolve(filename))).isFalse();
    }
}