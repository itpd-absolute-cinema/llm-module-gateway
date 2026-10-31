package ru.inno.itpd.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "gateway.llm.provider", havingValue = "stub", matchIfMissing = true)
public class StubLlmClient implements LlmClient {

    @Override
    public ChatCompletionResponse complete(ChatRequest request) {
        String lastUser = request.getMessages().stream()
                .filter(m -> Message.Role.valueOf("user").equals(m.getRole()))
                .reduce((a, b) -> b)
                .map(Message::getContent)
                .orElse("");

        Message reply = new Message();
        reply.setRole(Message.Role.valueOf("assistant"));
        reply.setContent("[stub] " + request.getMessages().stream()
                .map(Message::getContent)
                .collect(Collectors.joining("; ")));

        var choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(reply);
        choice.setFinishReason("stop");

        var usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(lastUser.split("\\s+").length);
        usage.setCompletionTokens(reply.getContent().split("\\s+").length);
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
}