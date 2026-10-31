package ru.inno.itpd.gateway.cucumber;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Map;

/** Sends real HTTP requests to the running app and remembers the last response. */
@Component
@ScenarioScope
public class ApiClient {
    @Value("${local.server.port}")
    private int port;

    private String token;
    private ResponseEntity<String> last;

    public void authenticate(String token) {
        this.token = token;
    }

    public void request(HttpMethod method, String path) {
        send(method, path, null, null);
    }

    public void json(HttpMethod method, String path, String body) {
        send(method, path, MediaType.APPLICATION_JSON, body);
    }

    public void upload(String filename, byte[] content, Map<String, String> fields) {
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new NamedResource(content, filename));
        fields.forEach(parts::add);
        send(HttpMethod.POST, "/admin/plugins", MediaType.MULTIPART_FORM_DATA, parts);
    }

    public int status() {
        return last.getStatusCode().value();
    }

    public String header(String name) {
        return last.getHeaders().getFirst(name);
    }

    public <T> T field(String jsonPath) {
        return JsonPath.read(last.getBody(), jsonPath);
    }

    private void send(HttpMethod method, String path, MediaType contentType, Object body) {
        RestClient.RequestBodySpec spec = client().method(method).uri(path);
        if (token != null) spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) spec = spec.contentType(contentType).body(body);
        // 4xx/5xx are valid outcomes here: keep them as a normal response
        last = spec.retrieve().onStatus(status -> true, (request, response) -> { }).toEntity(String.class);
    }

    private RestClient client() {
        var http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(http))
                .build();
    }

    private static class NamedResource extends ByteArrayResource {
        private final String filename;

        NamedResource(byte[] content, String filename) {
            super(content);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}