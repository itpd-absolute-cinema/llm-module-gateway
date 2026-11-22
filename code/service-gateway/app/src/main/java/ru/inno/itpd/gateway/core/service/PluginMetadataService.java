package ru.inno.itpd.gateway.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Достаёт из исходника плагина необязательные метаданные, НЕ исполняя код (только ast + literal_eval):
 * <ul>
 *   <li>{@code description} — атрибут класса плагина (или модуля, {@code DESCRIPTION}); иначе первая строка docstring класса;</li>
 *   <li>{@code options_schema} — JSON Schema для options: атрибут класса (или модуля, {@code OPTIONS_SCHEMA}),
 *       обязан быть литералом dict, сериализуемым в JSON;</li>
 *   <li>{@code metrics_spec} — объявление метрик плагина для Prometheus: атрибут класса (или модуля, {@code METRICS}),
 *       литерал dict. Содержимое проверяет Plugin Manager; шлюз только показывает объявленное в админке.</li>
 * </ul>
 */
@Service
public class PluginMetadataService {

    public record PluginMetadata(String description,
                                 Map<String, Object> optionsSchema, String optionsSchemaJson,
                                 Map<String, Object> metrics, String metricsJson) {
        public static final PluginMetadata EMPTY = new PluginMetadata(null, null, null, null, null);
    }

    private static final String SCRIPT = """
            import ast, json, sys
            src = sys.stdin.buffer.read()
            tree = ast.parse(src)
            out = {"description": None, "options_schema": None, "metrics_spec": None, "errors": {}}
            DICT_KEYS = {"options_schema": ("options_schema", "OPTIONS_SCHEMA"), "metrics_spec": ("metrics_spec", "METRICS")}

            def lit(node):
                try:
                    return ast.literal_eval(node)
                except Exception:
                    return ast.literal_eval  # marker: not a literal

            def is_plugin_class(c):
                bases = [getattr(b, "id", getattr(b, "attr", "")) for b in c.bases]
                has_apply = any(isinstance(n, ast.FunctionDef) and n.name == "apply" for n in c.body)
                return has_apply or "RequestFilter" in bases or "ResponseFilter" in bases

            def scan(body, module):
                for n in body:
                    if isinstance(n, ast.Assign):
                        names = [t.id for t in n.targets if isinstance(t, ast.Name)]
                        value = n.value
                    elif isinstance(n, ast.AnnAssign) and isinstance(n.target, ast.Name) and n.value is not None:
                        names = [n.target.id]
                        value = n.value
                    else:
                        continue
                    for name in names:
                        if name == ("DESCRIPTION" if module else "description") and out["description"] is None:
                            v = lit(value)
                            if isinstance(v, str):
                                out["description"] = v
                        for key, (cls_name, mod_name) in DICT_KEYS.items():
                            if name != (mod_name if module else cls_name):
                                continue
                            if out[key] is not None or key in out["errors"]:
                                continue
                            v = lit(value)
                            if not isinstance(v, dict):
                                out["errors"][key] = key + " must be a dict literal"
                                continue
                            try:
                                json.dumps(v)
                            except Exception:
                                out["errors"][key] = key + " must be JSON-serializable"
                                continue
                            out[key] = v

            classes = [c for c in tree.body if isinstance(c, ast.ClassDef)]
            plugin = next((c for c in classes if is_plugin_class(c)), None)
            if plugin is not None:
                scan(plugin.body, False)
            scan(tree.body, True)
            if out["description"] is None:
                doc = ast.get_docstring(plugin) if plugin is not None else ast.get_docstring(tree)
                if doc:
                    out["description"] = doc.strip().splitlines()[0]
            if out["description"] is not None:
                out["description"] = out["description"][:1000]
            print(json.dumps(out))
            """;

    private static final int MAX_JSON_BYTES = 64 * 1024;

    private final JsonMapper mapper;

    public PluginMetadataService(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /** Строгий режим для загрузки: некорректные options_schema / metrics_spec → 400. */
    public PluginMetadata extractStrict(byte[] source) {
        return extract(source, true);
    }

    /** Мягкий режим для бэкфилла старых плагинов: ошибки игнорируются. */
    public PluginMetadata extractLenient(byte[] source) {
        return extract(source, false);
    }

    private PluginMetadata extract(byte[] source, boolean strict) {
        Map<String, Object> raw;
        try {
            Process p = new ProcessBuilder("python3", "-c", SCRIPT).redirectErrorStream(false).start();
            try (OutputStream in = p.getOutputStream()) {
                in.write(source);
            }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return PluginMetadata.EMPTY;
            }
            if (p.exitValue() != 0) return PluginMetadata.EMPTY;
            raw = mapper.readValue(out.strip(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException | JacksonException e) {
            return PluginMetadata.EMPTY;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PluginMetadata.EMPTY;
        }

        Map<?, ?> errors = raw.get("errors") instanceof Map<?, ?> m ? m : Map.of();
        if (strict) {
            for (var entry : Map.of("options_schema", "invalid_options_schema",
                    "metrics_spec", "invalid_metrics_spec").entrySet()) {
                Object error = errors.get(entry.getKey());
                if (error != null) {
                    throw new GatewayException(HttpStatus.BAD_REQUEST, entry.getValue(),
                            "Invalid " + entry.getKey() + " in the plugin: " + error);
                }
            }
        }
        String description = raw.get("description") instanceof String s && !s.isBlank() ? s : null;
        Json schema = json(raw.get("options_schema"), "invalid_options_schema", "options_schema", strict);
        Json metrics = json(raw.get("metrics_spec"), "invalid_metrics_spec", "metrics_spec", strict);
        return new PluginMetadata(description, schema.map, schema.json, metrics.map, metrics.json);
    }

    private record Json(Map<String, Object> map, String json) {
        static final Json NONE = new Json(null, null);
    }

    @SuppressWarnings("unchecked")
    private Json json(Object value, String code, String field, boolean strict) {
        if (!(value instanceof Map<?, ?> map)) return Json.NONE;
        try {
            String json = mapper.writeValueAsString(map);
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
                if (strict) {
                    throw new GatewayException(HttpStatus.BAD_REQUEST, code, field + " is larger than 64 KiB");
                }
                return Json.NONE;
            }
            return new Json((Map<String, Object>) map, json);
        } catch (JacksonException e) {
            return Json.NONE;
        }
    }
}
