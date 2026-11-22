package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.inno.itpd.gateway.api.dto.ChatCompletionResponse;
import ru.inno.itpd.gateway.api.dto.ChatRequest;
import ru.inno.itpd.gateway.api.filter.RequestIdFilter;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.core.service.ChatService;
import ru.inno.itpd.gateway.core.service.StreamSession;

import java.io.IOException;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Endpoints for interacting with the chat service")
@SecurityRequirement(name = "HTTPBearer")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping(
            value = "/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE}
    )
    @Operation(
            summary = "Chat",
            description = "Generate a chat completion. Requires a Keycloak JWT; the user is identified by the `sub` claim. "
                    + "With stream=true the response is an SSE stream (text/event-stream).",
            operationId = "chat_v1_chat_completions_post"
    )
    public ChatCompletionResponse completions(@Valid @RequestBody ChatRequest request,
                                              @AuthenticationPrincipal Jwt jwt,
                                              HttpServletRequest http,
                                              HttpServletResponse response) throws IOException {
        String userId = jwt == null ? null : jwt.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "unauthorized", "The token has no subject claim");
        }
        String requestId = RequestIdFilter.of(http);

        if (request.isStreamRequested()) {
            // Все проверки и открытие потока у апстрима — до отправки заголовков: ошибки уходят обычным JSON.
            StreamSession session = service.openStream(userId, requestId, request);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");   // nginx: не буферизовать SSE
            // Блокирующая перекачка в потоке запроса; на виртуальных потоках (spring.threads.virtual.enabled) это дёшево.
            session.writeTo(response.getOutputStream());
            return null;
        }
        return service.complete(userId, requestId, request);
    }
}
