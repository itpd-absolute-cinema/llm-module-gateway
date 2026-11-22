package ru.inno.itpd.gateway.core.client.llm;

import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;
import ru.inno.itpd.gateway.api.dto.ChatRequest;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelInfo;

import java.util.List;

public interface LlmClient {
    ChatCompletionResponse complete(ChatRequest request);

    /** Открывает SSE-поток (stream:true). Ошибки апстрима выбрасываются до начала потока. */
    LlmStream stream(ChatRequest request);

    List<ModelInfo> listModels();
}
