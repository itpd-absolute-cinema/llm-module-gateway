package ru.inno.itpd.gateway.core.service;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.core.dao.entity.PricingDocument;
import ru.inno.itpd.gateway.core.dao.entity.UsageEventDocument;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** Пишет событие использования; стоимость считается в момент записи и больше не меняется. */
@Service
public class UsageRecorder {
    private static final Logger log = LoggerFactory.getLogger(UsageRecorder.class);

    public enum Status {
        OK("ok"), ERROR("error"), BLOCKED("blocked"), REJECTED_BY_PLUGIN("rejected_by_plugin");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Source {
        UPSTREAM("upstream"), ESTIMATED("estimated"), NONE("none");

        private final String value;

        Source(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public record Event(String requestId, String userId, String model, int promptTokens, int completionTokens,
                        Source source, long latencyMs, Status status, String errorCode, boolean stream) {
    }

    private final MongoTemplate mongo;
    private final PricingService pricing;

    public UsageRecorder(MongoTemplate mongo, PricingService pricing) {
        this.mongo = mongo;
        this.pricing = pricing;
    }

    public void record(Event e) {
        try {
            int total = e.promptTokens() + e.completionTokens();
            Double cost = null;
            String currency = null;
            var price = pricing.find(e.model());
            if (price.isPresent()) {
                PricingDocument p = price.get();
                currency = p.currency();
                cost = cost(p, e.promptTokens(), e.completionTokens());
            }
            mongo.insert(new UsageEventDocument(new ObjectId().toHexString(), Instant.now(), e.requestId(),
                    e.userId(), e.model(), e.promptTokens(), e.completionTokens(), total, e.latencyMs(),
                    e.status().value(), e.errorCode(), cost, currency, e.stream(), e.source().value()));
        } catch (RuntimeException ex) {
            // учёт не должен ломать ответ клиенту
            log.error("Failed to record usage event for request {}", e.requestId(), ex);
        }
    }

    static double cost(PricingDocument p, int promptTokens, int completionTokens) {
        BigDecimal in = BigDecimal.valueOf(p.inputPricePer1m()).multiply(BigDecimal.valueOf(promptTokens));
        BigDecimal out = BigDecimal.valueOf(p.outputPricePer1m()).multiply(BigDecimal.valueOf(completionTokens));
        return in.add(out).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP).doubleValue();
    }
}
