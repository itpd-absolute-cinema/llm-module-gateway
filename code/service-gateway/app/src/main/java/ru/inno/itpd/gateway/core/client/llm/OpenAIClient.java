package ru.inno.itpd.gateway.core.client.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;
import ru.inno.itpd.gateway.api.dto.ChatRequest;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelInfo;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelList;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/** OpenAI-совместимый апстрим (Ollama, vLLM и т.п.). */
@Component
@ConditionalOnProperty(
        name = "gateway.llm.provider",
        havingValue = "openai"
)
public class OpenAIClient implements LlmClient {
    private static final Duration HEADERS_TIMEOUT = Duration.ofSeconds(120);

    private final RestClient restClient;
    private final HttpClient http;
    private final String baseUrl;
    private final JsonMapper mapper;

    public OpenAIClient(
            RestClient.Builder restClientBuilder,
            JsonMapper mapper,
            @Value("${gateway.llm.base-url:http://localhost:11434}") String baseUrl
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.mapper = mapper;
        this.restClient = restClientBuilder.baseUrl(this.baseUrl).build();
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public ChatCompletionResponse complete(ChatRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RuntimeException e) {
            throw UpstreamErrors.map(e);
        }
    }

    @Override
    public LlmStream stream(ChatRequest request) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(HEADERS_TIMEOUT)   // до получения заголовков ответа; тело стримится без лимита
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)))
                    .build();
            HttpResponse<java.io.InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() >= 400) {
                try (var body = resp.body()) {
                    body.readNBytes(0);
                } catch (IOException ignored) {
                    // the status is all we need
                }
                throw UpstreamErrors.fromStatus(resp.statusCode());
            }
            return new LlmStream(resp.body());
        } catch (IOException e) {
            throw UpstreamErrors.fromIo(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw UpstreamErrors.fromIo(e);
        } catch (JacksonException e) {
            throw UpstreamErrors.map(new IllegalStateException(e));
        }
    }

    @Override
    public List<ModelInfo> listModels() {
        try {
            ModelList list = restClient.get()
                    .uri("/v1/models")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ModelList.class);
            return list == null || list.data() == null ? List.of() : list.data();
        } catch (RuntimeException e) {
            throw UpstreamErrors.map(e);
        }
    }
}
