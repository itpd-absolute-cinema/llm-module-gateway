package ru.inno.itpd.gateway.core.dao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/** user_id хранится как _id: уникальность и быстрый поиск на каждом запросе. */
@Document("blocked_users")
public record BlockedUserDocument(
        @Id String userId,
        String reason,
        @Field("blocked_at") Instant blockedAt,
        @Field("blocked_by") String blockedBy,
        /** true, если шлюз сам выключил учётку в Keycloak (тогда при разблокировке включит обратно). */
        @Field("keycloak_disabled") boolean keycloakDisabled
) {
}
