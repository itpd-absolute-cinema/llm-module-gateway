package ru.inno.itpd.gateway.core.client;

import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;
import ru.inno.itpd.gateway.api.dto.ChatRequest;

public interface LlmClient {
    ChatCompletionResponse complete(ChatRequest request);
}