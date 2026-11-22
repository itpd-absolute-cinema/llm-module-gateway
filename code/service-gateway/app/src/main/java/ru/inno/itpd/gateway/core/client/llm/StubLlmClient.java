package ru.inno.itpd.gateway.core.client.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;
import ru.inno.itpd.gateway.api.dto.ChatRequest;
import ru.inno.itpd.gateway.api.dto.Message;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelInfo;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Заглушка LLM для разработки и тестов. Особые модели:
 * <ul>
 *   <li>{@code stub-error} — апстрим отвечает ошибкой (502 upstream_error);</li>
 *   <li>{@code stub-no-usage} — в потоке нет usage-чанка (проверка собственного подсчёта токенов).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(
        name = "gateway.llm.provider",
        havingValue = "stub",
        matchIfMissing = true
)
public class StubLlmClient implements LlmClient {

    public static final String ERROR_MODEL = "stub-error";
    public static final String NO_USAGE_MODEL = "stub-no-usage";

    private final JsonMapper mapper;

    public StubLlmClient(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ChatCompletionResponse complete(ChatRequest request) {
        failIfRequested(request);
        String text = replyText(request);

        Message reply = new Message();
        reply.setRole(Message.Role.valueOf("assistant"));
        reply.setContent(text);

        var choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(reply);
        choice.setFinishReason("stop");

        var usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(promptTokens(request));
        usage.setCompletionTokens(words(text));
        usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());

        var response = new ChatCompletionResponse();
        response.setId("stub-" + UUID.randomUUID());
        response.setObject("chat.completion");
        response.setCreated(Instant.now().getEpochSecond());
        response.setModel(request.getModel());
        response.setChoices(List.of(choice));
        response.setUsage(usage);
        return response;
    }

    @Override
    public LlmStream stream(ChatRequest request) {
        failIfRequested(request);
        String text = replyText(request);
        String id = "stub-" + UUID.randomUUID();
        long created = Instant.now().getEpochSecond();

        List<Map<String, Object>> chunks = new ArrayList<>();
        chunks.add(chunk(id, created, request, List.of(choice(Map.of("role", "assistant", "content", ""), null))));
        for (String word : text.split("(?<= )")) {   // слова вместе с пробелом
            chunks.add(chunk(id, created, request, List.of(choice(Map.of("content", word), null))));
        }
        chunks.add(chunk(id, created, request, List.of(choice(Map.of(), "stop"))));

        boolean includeUsage = request.getStreamOptions() != null
                && Boolean.TRUE.equals(request.getStreamOptions().getIncludeUsage());
        if (includeUsage && !NO_USAGE_MODEL.equals(request.getModel())) {
            Map<String, Object> last = chunk(id, created, request, List.of());
            int prompt = promptTokens(request);
            int completion = words(text);
            last.put("usage", Map.of("prompt_tokens", prompt, "completion_tokens", completion,
                    "total_tokens", prompt + completion));
            chunks.add(last);
        }

        StringBuilder sse = new StringBuilder();
        for (Map<String, Object> c : chunks) {
            sse.append("data: ").append(mapper.writeValueAsString(c)).append("\n\n");
        }
        sse.append("data: [DONE]\n\n");
        return new LlmStream(new ByteArrayInputStream(sse.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public List<ModelInfo> listModels() {
        long now = Instant.now().getEpochSecond();
        return List.of(new ModelInfo("stub-model", "model", now, "stub"),
                new ModelInfo("gpt-test", "model", now, "stub"));
    }

    // ---- helpers ----

    private static void failIfRequested(ChatRequest request) {
        if (ERROR_MODEL.equals(request.getModel())) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_error", "The LLM responded with status 500");
        }
    }

    private static String replyText(ChatRequest request) {
        return "[stub] " + request.getMessages().stream()
                .map(Message::getContent)
                .collect(Collectors.joining("; "));
    }

    private static int promptTokens(ChatRequest request) {
        String lastUser = request.getMessages().stream()
                .filter(m -> Message.Role.valueOf("user").equals(m.getRole()))
                .reduce((_, b) -> b)
                .map(Message::getContent)
                .orElse("");
        return words(lastUser);
    }

    private static int words(String s) {
        return s.split("\\s+").length;
    }

    private static Map<String, Object> choice(Map<String, Object> delta, String finishReason) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("index", 0);
        c.put("delta", delta);
        c.put("finish_reason", finishReason);
        return c;
    }

    private static Map<String, Object> chunk(String id, long created, ChatRequest request,
                                             List<Map<String, Object>> choices) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("object", "chat.completion.chunk");
        m.put("created", created);
        m.put("model", request.getModel());
        m.put("choices", choices);
        return m;
    }
}
