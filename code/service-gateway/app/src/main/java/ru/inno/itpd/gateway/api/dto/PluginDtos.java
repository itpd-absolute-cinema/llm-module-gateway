package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public final class PluginDtos {
    private PluginDtos() {
    }


    public record PluginInfo(String name, PluginType type, boolean enabled, Map<String, Object> options) {
    }

    /**
     * Представление плагина для админ-API: PluginInfo + метаданные из исходника + позиция в цепочке.
     * PluginInfo остаётся «чистым» — он же уходит в Plugin Manager.
     */
    public record PluginView(
            String name,
            PluginType type,
            boolean enabled,
            Map<String, Object> options,
            int position,
            @JsonInclude(JsonInclude.Include.NON_NULL) String description,
            @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("options_schema") Map<String, Object> optionsSchema,
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> metrics
    ) {
    }

    public record Settings(Map<String, Object> options, String before, String after) {
    }

    public record Status(Boolean enabled, String before, String after) {
    }

    public record ErrorDetail(String message, String type, Object param, String code) {
    }

    public record ErrorResponse(ErrorDetail error) {
        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(new ErrorDetail(message, "gateway_error", null, code));
        }
    }
}