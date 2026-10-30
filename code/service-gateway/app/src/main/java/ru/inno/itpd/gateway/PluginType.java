package ru.inno.itpd.gateway;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PluginType {
    REQUEST_FILTER("request_filter"),
    RESPONSE_FILTER("response_filter");

    private final String value;

    PluginType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}