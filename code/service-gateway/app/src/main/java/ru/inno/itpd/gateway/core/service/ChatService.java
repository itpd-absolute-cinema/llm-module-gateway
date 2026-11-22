package ru.inno.itpd.gateway.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;
import ru.inno.itpd.gateway.api.dto.ChatRequest;
import ru.inno.itpd.gateway.api.dto.PluginType;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.client.llm.LlmClient;
import ru.inno.itpd.gateway.core.client.llm.LlmStream;
import ru.inno.itpd.gateway.core.client.pluginmanager.PluginManagerClient;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.core.service.UsageRecorder.Source;
import ru.inno.itpd.gateway.core.service.UsageRecorder.Status;
import ru.inno.itpd.gateway.util.TokenEstimator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Прогоняет запрос через блокировку → request-фильтры → LLM → response-фильтры и пишет событие
 * использования на КАЖДЫЙ исход: ok, error, blocked, rejected_by_plugin.
 */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PluginService plugins;
    private final PluginManagerClient pluginManager;
    private final LlmClient llm;
    private final BlockService blocks;
    private final UsageRecorder usage;
    private final GatewayProperties props;
    private final JsonMapper mapper;

    public ChatService(PluginService plugins, PluginManagerClient pluginManager, LlmClient llm, BlockService blocks,
                       UsageRecorder usage, GatewayProperties props, JsonMapper mapper) {
        this.plugins = plugins;
        this.pluginManager = pluginManager;
        this.llm = llm;
        this.blocks = blocks;
        this.usage = usage;
        this.props = props;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- обычный запрос

    public ChatCompletionResponse complete(String userId, String requestId, ChatRequest request) {
        long started = System.nanoTime();
        String model = request.getModel();
        ChatCompletionResponse raw = null;
        try {
            blocks.assertNotBlocked(userId);

            log.debug("Incoming request: {}", request);
            ChatRequest filtered = pluginManager.filterRequest(request, plugins.enabledChain(PluginType.REQUEST_FILTER));
            model = filtered.getModel();   // плагин мог перенаправить запрос на другую модель — тарифицируем её
            log.debug("Filtered request: {}", filtered);

            raw = llm.complete(filtered);
            log.debug("Completed request: {}", raw);

            ChatCompletionResponse result = pluginManager.filterResponse(
                    filtered, raw, plugins.enabledChain(PluginType.RESPONSE_FILTER));
            log.debug("Filtered response: {}", result);

            Tokens t = tokens(raw, filtered);
            usage.record(new UsageRecorder.Event(requestId, userId, model, t.prompt, t.completion, t.source,
                    elapsedMs(started), Status.OK, null, false));
            return result;
        } catch (GatewayException e) {
            recordFailure(requestId, userId, model, raw, request, started, e.code(), false);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(requestId, userId, model, raw, request, started, "internal_error", false);
            throw e;
        }
    }

    // ---------------------------------------------------------------- stream:true

    /**
     * Синхронная часть стрима: блокировка, фильтры запроса, открытие потока у апстрима. Любая ошибка здесь
     * выбрасывается ДО отправки заголовков, поэтому клиент получает обычный JSON-ответ с ошибкой.
     *
     * <p>Учёт токенов: шлюз всегда просит у апстрима {@code stream_options.include_usage=true} и берёт usage из
     * последнего чанка. Если апстрим его не прислал — токены оцениваются ({@link TokenEstimator}) и событие
     * помечается {@code usage_source=estimated}. Usage-чанк клиенту пересылается только если он сам запросил
     * include_usage.
     *
     * <p>Response-фильтры не умеют работать с потоком; по умолчанию стрим при включённых response-фильтрах
     * отклоняется (fail closed), см. gateway.streaming.reject-with-response-filters.
     */
    public StreamSession openStream(String userId, String requestId, ChatRequest request) {
        long started = System.nanoTime();
        String model = request.getModel();
        boolean clientWantsUsage = request.getStreamOptions() != null
                && Boolean.TRUE.equals(request.getStreamOptions().getIncludeUsage());
        try {
            blocks.assertNotBlocked(userId);

            if (!plugins.enabledChain(PluginType.RESPONSE_FILTER).isEmpty()) {
                if (props.streaming().rejectWithResponseFilters()) {
                    throw new GatewayException(HttpStatus.BAD_REQUEST, "stream_not_supported_with_response_filters",
                            "Streaming is not available while response filters are enabled");
                }
                log.warn("Streaming request {}: response filters are skipped", requestId);
            }

            ChatRequest filtered = pluginManager.filterRequest(request, plugins.enabledChain(PluginType.REQUEST_FILTER));
            model = filtered.getModel();
            filtered.setStream(true);
            filtered.setStreamOptions(new ChatRequest.StreamOptions(true));

            LlmStream upstream = llm.stream(filtered);
            return new Pump(upstream, filtered, userId, requestId, model, clientWantsUsage, started);
        } catch (GatewayException e) {
            recordFailure(requestId, userId, model, null, request, started, e.code(), true);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(requestId, userId, model, null, request, started, "internal_error", true);
            throw e;
        }
    }

    /** Перекачка SSE от апстрима клиенту с подсчётом usage. */
    private final class Pump implements StreamSession {
        private final LlmStream upstream;
        private final ChatRequest sent;
        private final String userId;
        private final String requestId;
        private final String model;
        private final boolean clientWantsUsage;
        private final long started;

        Pump(LlmStream upstream, ChatRequest sent, String userId, String requestId, String model,
             boolean clientWantsUsage, long started) {
            this.upstream = upstream;
            this.sent = sent;
            this.userId = userId;
            this.requestId = requestId;
            this.model = model;
            this.clientWantsUsage = clientWantsUsage;
            this.started = started;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            int[] upstreamUsage = null;   // prompt, completion
            StringBuilder text = new StringBuilder();
            Status status = Status.OK;
            String errorCode = null;
            boolean clientGone = false;

            try (upstream;
                 var reader = new BufferedReader(new InputStreamReader(upstream.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String payload = line.substring(5).strip();
                    if (payload.isEmpty()) continue;

                    boolean forward = true;
                    if (!payload.equals("[DONE]")) {
                        try {
                            JsonNode node = mapper.readTree(payload);
                            JsonNode u = node.get("usage");
                            if (u != null && u.isObject()) {
                                upstreamUsage = new int[]{u.path("prompt_tokens").asInt(0), u.path("completion_tokens").asInt(0)};
                                // отдельный usage-чанк (choices пуст) нужен клиенту, только если он просил include_usage
                                if (node.path("choices").size() == 0 && !clientWantsUsage) forward = false;
                            }
                            for (JsonNode choice : node.path("choices")) {
                                String piece = choice.path("delta").path("content").asString("");
                                text.append(piece);
                            }
                        } catch (JacksonException ignored) {
                            // не JSON — пересылаем как есть, в подсчёт не берём
                        }
                    }
                    if (forward) {
                        try {
                            out.write(("data: " + payload + "\n\n").getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        } catch (IOException e) {
                            clientGone = true;
                            throw e;
                        }
                    }
                    if (payload.equals("[DONE]")) break;
                }
            } catch (IOException e) {
                status = Status.ERROR;
                if (clientGone) {
                    errorCode = "client_aborted";
                } else {
                    errorCode = "upstream_error";
                    log.warn("Upstream stream for request {} broke: {}", requestId, e.toString());
                    trySendError(out);
                }
            } finally {
                Tokens t;
                if (upstreamUsage != null) {
                    t = new Tokens(upstreamUsage[0], upstreamUsage[1], Source.UPSTREAM);
                } else {
                    t = new Tokens(TokenEstimator.estimatePrompt(sent.getMessages()),
                            TokenEstimator.estimate(text), Source.ESTIMATED);
                }
                usage.record(new UsageRecorder.Event(requestId, userId, model, t.prompt, t.completion, t.source,
                        elapsedMs(started), status, errorCode, true));
            }
        }

        private void trySendError(OutputStream out) {
            try {
                out.write(("data: {\"error\":{\"message\":\"The LLM stream was interrupted\","
                        + "\"type\":\"gateway_error\",\"param\":null,\"code\":\"upstream_error\"}}\n\n")
                        .getBytes(StandardCharsets.UTF_8));
                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException ignored) {
                // клиент уже ушёл
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private record Tokens(int prompt, int completion, Source source) {
    }

    /** usage апстрима; если его нет — оценка по тексту. */
    private static Tokens tokens(ChatCompletionResponse raw, ChatRequest sent) {
        var u = raw.getUsage();
        if (u != null) return new Tokens(u.getPromptTokens(), u.getCompletionTokens(), Source.UPSTREAM);
        StringBuilder sb = new StringBuilder();
        if (raw.getChoices() != null) {
            raw.getChoices().forEach(c -> {
                if (c.getMessage() != null && c.getMessage().getContent() != null) sb.append(c.getMessage().getContent());
            });
        }
        return new Tokens(TokenEstimator.estimatePrompt(sent.getMessages()), TokenEstimator.estimate(sb), Source.ESTIMATED);
    }

    private void recordFailure(String requestId, String userId, String model, ChatCompletionResponse raw,
                               ChatRequest request, long started, String code, boolean stream) {
        Status status = switch (code) {
            case "user_blocked" -> Status.BLOCKED;
            case "plugin_rejected" -> Status.REJECTED_BY_PLUGIN;
            default -> Status.ERROR;
        };
        // если ответ LLM уже получен (например, отклонил response-фильтр), токены уже потрачены
        Tokens t = raw != null ? tokens(raw, request) : new Tokens(0, 0, Source.NONE);
        usage.record(new UsageRecorder.Event(requestId, userId, model, t.prompt, t.completion, t.source,
                elapsedMs(started), status, code, stream));
    }

    private static long elapsedMs(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
