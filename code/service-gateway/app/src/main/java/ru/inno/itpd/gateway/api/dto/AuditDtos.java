package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public final class AuditDtos {
    private AuditDtos() {}

    public record AuditEntry(
            Instant timestamp,
            String actor,
            @JsonProperty("actor_username") String actorUsername,
            String action,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("target_id") String targetId,
            Object before,
            Object after,
            @JsonProperty("request_id") String requestId
    ) {
    }
}
