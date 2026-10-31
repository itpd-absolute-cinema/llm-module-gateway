package ru.inno.itpd.gateway;

public interface LlmClient {
    ChatCompletionResponse complete(ChatRequest request);
}