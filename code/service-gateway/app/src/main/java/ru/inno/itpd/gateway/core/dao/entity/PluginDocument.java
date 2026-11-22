package ru.inno.itpd.gateway.core.dao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.inno.itpd.gateway.api.dto.PluginType;

import java.util.Map;

@Document("plugins")
public class PluginDocument {
    @Id
    private String name;                  // = _id
    private PluginType type;              // задаётся при создании, дальше не меняется
    private boolean enabled;
    private Map<String, Object> options;  // settings плагина
    private int position;                 // порядок в цепочке
    private String description;           // из исходника плагина (может отсутствовать)
    private String optionsSchemaJson;     // JSON Schema options как строка: ключи вроде $schema/$ref небезопасны для Mongo
    private String metricsJson;           // metrics_spec плагина (объявление метрик) как JSON-строка
    private int metadataVersion;          // версия извлечения метаданных; 0 — не извлекались (старые плагины)

    public PluginDocument() {
    }

    public PluginDocument(String name, PluginType type, boolean enabled, Map<String, Object> options, int position) {
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.options = options;
        this.position = position;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PluginType getType() { return type; }
    public void setType(PluginType type) { this.type = type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOptionsSchemaJson() { return optionsSchemaJson; }
    public void setOptionsSchemaJson(String optionsSchemaJson) { this.optionsSchemaJson = optionsSchemaJson; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public int getMetadataVersion() { return metadataVersion; }
    public void setMetadataVersion(int metadataVersion) { this.metadataVersion = metadataVersion; }
}
