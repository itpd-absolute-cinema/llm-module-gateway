package ru.inno.itpd.gateway.core.client.llm;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.inno.itpd.gateway.core.exception.GatewayException;

import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;

final class UpstreamErrors {
    private UpstreamErrors() {
    }

    static GatewayException fromStatus(int status) {
        return switch (status) {
            case 400 -> new GatewayException(HttpStatus.BAD_REQUEST, "upstream_bad_request", "The LLM rejected the request");
            case 404 -> new GatewayException(HttpStatus.NOT_FOUND, "upstream_not_found", "The LLM does not know this model or endpoint");
            case 429 -> new GatewayException(HttpStatus.TOO_MANY_REQUESTS, "upstream_rate_limited", "The LLM is rate limiting requests");
            default -> new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_error", "The LLM responded with status " + status);
        };
    }

    static GatewayException map(RuntimeException e) {
        if (e instanceof GatewayException g) return g;
        if (e instanceof RestClientResponseException r) return fromStatus(r.getStatusCode().value());
        if (e instanceof ResourceAccessException r) return fromIo(r.getCause());
        return new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_error", "The LLM request failed");
    }

    static GatewayException fromIo(Throwable cause) {
        if (cause instanceof HttpTimeoutException || cause instanceof InterruptedIOException) {
            return new GatewayException(HttpStatus.GATEWAY_TIMEOUT, "upstream_timeout", "The LLM request timed out");
        }
        return new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_unreachable", "Could not connect to the LLM");
    }
}
