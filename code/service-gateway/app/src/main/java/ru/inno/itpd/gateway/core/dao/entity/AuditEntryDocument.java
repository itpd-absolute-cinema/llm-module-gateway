package ru.inno.itpd.gateway.core.dao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/** before/after хранятся как JSON-строки: схема произвольна, а ключи могут содержать точки и $. */
@Document(AuditEntryDocument.COLLECTION)
public record AuditEntryDocument(
        @Id String id,
        Instant timestamp,
        String actor,
        @Field("actor_username") String actorUsername,
        String action,
        @Field("target_type") String targetType,
        @Field("target_id") String targetId,
        @Field("before_json") String beforeJson,
        @Field("after_json") String afterJson,
        @Field("request_id") String requestId
) {
    public static final String COLLECTION = "audit_log";
}
