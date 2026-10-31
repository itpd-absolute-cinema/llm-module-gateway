package ru.inno.itpd.gateway;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;


@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PluginUtils {

    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static String requireValidName(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_plugin_name", "Invalid plugin name. Plugin name must match regex [A-Za-z_][A-Za-z0-9_]*");
        }
        return name;
    }

    public static String nameFromFilename(String filename) {
        if (filename == null || !filename.endsWith(".py")) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_filename", "Invalid filename. Expected a .py file");
        }
        return requireValidName(filename.substring(0, filename.length() - 3));
    }

    public static Map<String, Object> parseOptions(String json) {
        Map<String, Object> options;
        try {
            options = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException e) {
            options = null;
        }
        if (options == null) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_options", "Options must be a JSON object");
        }
        return checkOptions(options);
    }

    /** 400 если options отсутствует, 422 если внутри есть null (в TOML/Mongo-конфиге его не представить). */
    public static Map<String, Object> checkOptions(Map<String, Object> options) {
        if (options == null) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "malformed_request", "'options' is required");
        }
        if (containsNull(options)) {
            throw new GatewayException(HttpStatus.UNPROCESSABLE_ENTITY, "options_not_representable",
                    "Options cannot be represented in TOML, for example JSON null");
        }
        return options;
    }

    public static boolean containsNull(Object o) {
        if (o == null) return true;
        if (o instanceof Map<?, ?> m) return m.values().stream().anyMatch(PluginUtils::containsNull);
        if (o instanceof Collection<?> c) return c.stream().anyMatch(PluginUtils::containsNull);
        return false;
    }

    public static <T> ResponseEntity<T> response(HttpStatus status, T body, boolean restartRequired) {
        return ResponseEntity.status(status)
                .header("X-Restart-Required", String.valueOf(restartRequired))
                .body(body);
    }

    public static PluginType parseType(String value) {
        for (PluginType t : PluginType.values()) {
            if (t.value().equals(value)) return t;
        }
        throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_plugin_type",
                "Plugin type must be 'request_filter' or 'response_filter'");
    }
}