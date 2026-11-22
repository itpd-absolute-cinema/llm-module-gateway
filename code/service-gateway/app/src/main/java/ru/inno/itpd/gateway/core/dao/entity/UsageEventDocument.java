package ru.inno.itpd.gateway.core.dao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/** Событие использования: одна запись на каждый запрос к /chat/completions (успешный или нет). */
@Document(UsageEventDocument.COLLECTION)
public record UsageEventDocument(
        @Id String id,
        Instant timestamp,
        @Field("request_id") String requestId,
        @Field("user_id") String userId,
        String model,
        @Field("prompt_tokens") int promptTokens,
        @Field("completion_tokens") int completionTokens,
        @Field("total_tokens") int totalTokens,
        @Field("latency_ms") long latencyMs,
        String status,
        @Field("error_code") String errorCode,
        /** Стоимость на момент записи; null, если для модели нет цены. */
        Double cost,
        String currency,
        boolean stream,
        /** upstream — usage от апстрима, estimated — посчитан шлюзом, none — токенов нет. */
        @Field("usage_source") String usageSource
) {
    public static final String COLLECTION = "usage_events";
}
