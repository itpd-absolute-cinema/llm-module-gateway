package ru.inno.itpd.gateway.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.ChatRequest;
import ru.inno.itpd.gateway.api.dto.PluginType;
import ru.inno.itpd.gateway.core.client.LlmClient;
import ru.inno.itpd.gateway.core.client.PluginManagerClient;
import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final PluginService plugins;
    private final PluginManagerClient pluginManager;
    private final LlmClient llm;

    public ChatCompletionResponse complete(ChatRequest request) {

        log.debug("Incoming request: {}", request);

        ChatRequest filtered = pluginManager.filterRequest(
                request, plugins.enabledChain(PluginType.REQUEST_FILTER));

        log.debug("Filtered request: {}", filtered);

        ChatCompletionResponse raw = llm.complete(filtered);

        log.debug("Completed request: {}", raw);

        ChatCompletionResponse chatCompletionResponse = pluginManager.filterResponse(
                filtered, raw, plugins.enabledChain(PluginType.RESPONSE_FILTER));

        log.debug("Filtered response: {}", chatCompletionResponse);

        return chatCompletionResponse;
    }
}