package ru.inno.itpd.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import ru.inno.itpd.gateway.PluginDtos.ErrorResponse;
import ru.inno.itpd.gateway.PluginDtos.PluginInfo;
import ru.inno.itpd.gateway.PluginManagerDtos.RequestFiltersInput;
import ru.inno.itpd.gateway.PluginManagerDtos.ResponseFiltersInput;

import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.List;

@Component
public class PluginManagerClient {
    private final RestClient client;

    public PluginManagerClient(GatewayProperties props) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(props.pluginManagerTimeout());
        this.client = RestClient.builder()
                .baseUrl(props.pluginManagerUrl())
                .requestFactory(factory)
                .build();
    }

    public ChatRequest filterRequest(ChatRequest request, List<PluginInfo> plugins) {
        if (plugins.isEmpty()) return request;
        return post("/request-filters", new RequestFiltersInput(request, plugins), ChatRequest.class);
    }

    public ChatCompletionResponse filterResponse(ChatRequest request, ChatCompletionResponse response,
                                                 List<PluginInfo> plugins) {
        if (plugins.isEmpty()) return response;
        return post("/response-filters", new ResponseFiltersInput(request, response, plugins),
                ChatCompletionResponse.class);
    }

    private <T> T post(String path, Object payload, Class<T> type) {
        try {
            T body = client.post().uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(type);
            if (body == null) {
                throw new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_error",
                        "Plugin manager returned an empty response");
            }
            return body;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 400 || status == 403) {
                // a plugin rejected the request/response: pass its verdict on to the client
                throw new GatewayException(HttpStatus.valueOf(status), "plugin_rejected", rejectionMessage(e));
            }
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_error",
                    "Plugin manager returned an error response");
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof HttpTimeoutException || cause instanceof InterruptedIOException) {
                throw new GatewayException(HttpStatus.GATEWAY_TIMEOUT, "upstream_timeout",
                        "The plugin manager request timed out");
            }
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "upstream_unreachable",
                    "Could not connect to plugin manager");
        }
    }

    private String rejectionMessage(RestClientResponseException e) {
        try {
            ErrorResponse body = e.getResponseBodyAs(ErrorResponse.class);
            if (body != null && body.error() != null && body.error().message() != null) {
                return body.error().message();
            }
        } catch (RuntimeException ignored) {
            // fall through to the generic message
        }
        return "Rejected by a plugin";
    }
}