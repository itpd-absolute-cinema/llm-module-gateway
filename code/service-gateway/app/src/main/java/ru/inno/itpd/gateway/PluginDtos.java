package ru.inno.itpd.gateway;

import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PluginDtos {

    public record PluginInfo(String name, PluginType type, boolean enabled, Map<String, Object> options) {
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