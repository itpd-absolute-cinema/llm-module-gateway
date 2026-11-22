package ru.inno.itpd.gateway.core.dao;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import ru.inno.itpd.gateway.core.dao.entity.AuditEntryDocument;
import ru.inno.itpd.gateway.core.dao.entity.UsageEventDocument;

/**
 * Индексы под запросы админ-API. Создаются при старте (идемпотентно), а не через auto-index-creation,
 * чтобы не зависеть от настроек Spring Data.
 */
@Component
public class MongoIndexInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongo;

    public MongoIndexInitializer(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            var usage = mongo.getCollection(UsageEventDocument.COLLECTION);
            // выборки/агрегации всегда ограничены по времени, остальные фильтры — вторым ключом
            usage.createIndex(Indexes.descending("timestamp"), new IndexOptions().name("ts_desc"));
            usage.createIndex(Indexes.compoundIndex(Indexes.ascending("user_id"), Indexes.descending("timestamp")),
                    new IndexOptions().name("user_ts"));
            usage.createIndex(Indexes.compoundIndex(Indexes.ascending("model"), Indexes.descending("timestamp")),
                    new IndexOptions().name("model_ts"));
            usage.createIndex(Indexes.compoundIndex(Indexes.ascending("status"), Indexes.descending("timestamp")),
                    new IndexOptions().name("status_ts"));
            usage.createIndex(Indexes.ascending("request_id"), new IndexOptions().name("request_id_uq").unique(true));

            var audit = mongo.getCollection(AuditEntryDocument.COLLECTION);
            audit.createIndex(Indexes.descending("timestamp"), new IndexOptions().name("ts_desc"));
            audit.createIndex(Indexes.compoundIndex(Indexes.ascending("actor"), Indexes.descending("timestamp")),
                    new IndexOptions().name("actor_ts"));
            audit.createIndex(Indexes.compoundIndex(Indexes.ascending("action"), Indexes.descending("timestamp")),
                    new IndexOptions().name("action_ts"));
        } catch (RuntimeException e) {
            log.error("Could not create MongoDB indexes; admin queries may be slow", e);
        }
    }
}
