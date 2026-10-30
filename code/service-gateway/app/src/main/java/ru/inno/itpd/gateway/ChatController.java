
package ru.inno.itpd.gateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints for interacting with the chat service")
public class ChatController {

    private final ChatService service;

    @PostMapping(
            value = "/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Chat",
            description = "Generate a chat completion based on the provided request",
            operationId = "chat_v1_chat_completions_post"
    )
    public ChatCompletionResponse completions(@Valid @RequestBody ChatRequest request) {
        return service.complete(request);
    }
}
