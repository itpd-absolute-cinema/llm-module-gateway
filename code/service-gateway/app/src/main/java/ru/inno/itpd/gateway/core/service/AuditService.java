package ru.inno.itpd.gateway.core.service;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.AuditDtos.AuditEntry;
import ru.inno.itpd.gateway.api.dto.PageResponse;
import ru.inno.itpd.gateway.api.filter.RequestIdFilter;
import ru.inno.itpd.gateway.core.dao.entity.AuditEntryDocument;
import ru.inno.itpd.gateway.core.security.Actor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Журнал действий администраторов: кто, что, когда, значение до и после. */
@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final MongoTemplate mongo;
    private final JsonMapper mapper;

    public AuditService(MongoTemplate mongo, JsonMapper mapper) {
        this.mongo = mongo;
        this.mapper = mapper;
    }

    /**
     * Пишет запись от имени текущего администратора. Сбой записи логируется на уровне ERROR,
     * но не отменяет уже выполненную операцию.
     */
    public void record(String action, String targetType, String targetId, Object before, Object after) {
        try {
            Actor actor = Actor.current();
            mongo.insert(new AuditEntryDocument(
                    new ObjectId().toHexString(), Instant.now(), actor.id(), actor.username(),
                    action, targetType, targetId, toJson(before), toJson(after), RequestIdFilter.current()));
        } catch (RuntimeException e) {
            log.error("Failed to write audit entry {} {} {}", action, targetType, targetId, e);
        }
    }

    public PageResponse<AuditEntry> list(Instant from, Instant to, String actor, String action, int page, int size) {
        List<Criteria> and = new ArrayList<>();
        and.add(Criteria.where("timestamp").gte(java.util.Date.from(from)).lt(java.util.Date.from(to)));
        if (actor != null) {
            and.add(new Criteria().orOperator(Criteria.where("actor").is(actor),
                    Criteria.where("actor_username").is(actor)));
        }
        if (action != null) and.add(Criteria.where("action").is(action));
        Query filter = new Query(new Criteria().andOperator(and));

        long total = mongo.count(filter, AuditEntryDocument.class);
        Query paged = Query.of(filter).with(Sort.by(Sort.Direction.DESC, "timestamp", "id")).skip((long) page * size).limit(size);
        List<AuditEntry> items = mongo.find(paged, AuditEntryDocument.class).stream().map(this::view).toList();
        return new PageResponse<>(items, page, size, total);
    }

    private AuditEntry view(AuditEntryDocument d) {
        return new AuditEntry(d.timestamp(), d.actor(), d.actorUsername(), d.action(), d.targetType(), d.targetId(),
                fromJson(d.beforeJson()), fromJson(d.afterJson()), d.requestId());
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            return null;
        }
    }

    private Object fromJson(String json) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, Object.class);
        } catch (JacksonException e) {
            return null;
        }
    }
}
