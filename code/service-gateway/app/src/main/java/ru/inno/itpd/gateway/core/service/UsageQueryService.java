package ru.inno.itpd.gateway.core.service;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.PageResponse;
import ru.inno.itpd.gateway.api.dto.UsageDtos.Timeseries;
import ru.inno.itpd.gateway.api.dto.UsageDtos.TimeseriesPoint;
import ru.inno.itpd.gateway.api.dto.UsageDtos.UsageEventView;
import ru.inno.itpd.gateway.api.dto.UsageDtos.UsageSummary;
import ru.inno.itpd.gateway.core.dao.entity.UsageEventDocument;
import ru.inno.itpd.gateway.core.exception.GatewayException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Чтение учёта: сводка, временной ряд, события, экспорт, последняя активность. */
@Service
public class UsageQueryService {
    private static final long MAX_BUCKETS = 5000;

    /** Фильтр: [from, to) по времени плюс необязательные user_id / model / status. */
    public record Filter(Instant from, Instant to, String userId, String model, String status) {
        Document match() {
            Document m = new Document("timestamp", new Document("$gte", Date.from(from)).append("$lt", Date.from(to)));
            if (userId != null) m.append("user_id", userId);
            if (model != null) m.append("model", model);
            if (status != null) m.append("status", status);
            return m;
        }
    }

    private final MongoTemplate mongo;
    private final PricingService pricing;

    public UsageQueryService(MongoTemplate mongo, PricingService pricing) {
        this.mongo = mongo;
        this.pricing = pricing;
    }

    public UsageSummary summary(Filter f) {
        Document totals = first(aggregate(List.of(
                new Document("$match", f.match()),
                new Document("$group", new Document("_id", null)
                        .append("requests", new Document("$sum", 1))
                        .append("prompt_tokens", new Document("$sum", "$prompt_tokens"))
                        .append("completion_tokens", new Document("$sum", "$completion_tokens"))
                        .append("total_tokens", new Document("$sum", "$total_tokens"))
                        .append("cost", new Document("$sum", "$cost"))
                        .append("avg_latency_ms", new Document("$avg", "$latency_ms"))))));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String s : List.of("ok", "error", "blocked", "rejected_by_plugin")) byStatus.put(s, 0L);
        for (Document d : aggregate(List.of(
                new Document("$match", f.match()),
                new Document("$group", new Document("_id", "$status").append("n", new Document("$sum", 1)))))) {
            byStatus.put(String.valueOf(d.get("_id")), lng(d.get("n")));
        }

        long requests = lng(totals.get("requests"));
        long errors = byStatus.entrySet().stream().filter(e -> !e.getKey().equals("ok"))
                .mapToLong(Map.Entry::getValue).sum();
        return new UsageSummary(f.from(), f.to(), requests, errors,
                requests == 0 ? 0 : (double) errors / requests,
                lng(totals.get("prompt_tokens")), lng(totals.get("completion_tokens")), lng(totals.get("total_tokens")),
                dbl(totals.get("cost")), pricing.currency(), dbl(totals.get("avg_latency_ms")), byStatus);
    }

    public Timeseries timeseries(Filter f, String interval, String groupBy) {
        Duration step = interval.equals("hour") ? Duration.ofHours(1) : Duration.ofDays(1);
        if (Duration.between(f.from(), f.to()).dividedBy(step) > MAX_BUCKETS) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "range_too_large",
                    "The range would produce more than " + MAX_BUCKETS + " buckets; narrow it or use interval=day");
        }
        Document id = new Document("bucket", new Document("$dateTrunc",
                new Document("date", "$timestamp").append("unit", interval).append("timezone", "UTC")));
        if (groupBy != null) id.append("group", groupBy.equals("user") ? "$user_id" : "$model");

        List<Document> rows = aggregate(List.of(
                new Document("$match", f.match()),
                new Document("$group", new Document("_id", id)
                        .append("requests", new Document("$sum", 1))
                        .append("errors", new Document("$sum", new Document("$cond",
                                List.of(new Document("$eq", List.of("$status", "ok")), 0, 1))))
                        .append("prompt_tokens", new Document("$sum", "$prompt_tokens"))
                        .append("completion_tokens", new Document("$sum", "$completion_tokens"))
                        .append("total_tokens", new Document("$sum", "$total_tokens"))
                        .append("cost", new Document("$sum", "$cost"))),
                new Document("$sort", new Document("_id.bucket", 1).append("_id.group", 1))));

        List<TimeseriesPoint> points = new ArrayList<>();
        for (Document d : rows) {
            Document key = (Document) d.get("_id");
            Object group = key.get("group");
            points.add(new TimeseriesPoint(((Date) key.get("bucket")).toInstant(),
                    group == null ? null : group.toString(),
                    lng(d.get("requests")), lng(d.get("errors")), lng(d.get("prompt_tokens")),
                    lng(d.get("completion_tokens")), lng(d.get("total_tokens")), dbl(d.get("cost"))));
        }
        return new Timeseries(f.from(), f.to(), interval, groupBy, points);
    }

    public PageResponse<UsageEventView> events(Filter f, int page, int size) {
        Query base = new BasicQuery(f.match());
        long total = mongo.count(base, UsageEventDocument.class);
        Query paged = new BasicQuery(f.match())
                .with(Sort.by(Sort.Direction.DESC, "timestamp", "id"))
                .skip((long) page * size).limit(size);
        List<UsageEventView> items = mongo.find(paged, UsageEventDocument.class).stream()
                .map(UsageQueryService::view).toList();
        return new PageResponse<>(items, page, size, total);
    }

    /** Потоковая выборка для CSV (по возрастанию времени). Поток нужно закрыть. */
    public Stream<UsageEventView> stream(Filter f) {
        Query q = new BasicQuery(f.match()).with(Sort.by(Sort.Direction.ASC, "timestamp"));
        return mongo.stream(q, UsageEventDocument.class).map(UsageQueryService::view);
    }

    /** Время последнего запроса (любого статуса) по каждому пользователю из списка. */
    public Map<String, Instant> lastActivity(Collection<String> userIds) {
        Map<String, Instant> result = new LinkedHashMap<>();
        if (userIds.isEmpty()) return result;
        for (Document d : aggregate(List.of(
                new Document("$match", new Document("user_id", new Document("$in", new ArrayList<>(userIds)))),
                new Document("$group", new Document("_id", "$user_id").append("last", new Document("$max", "$timestamp")))))) {
            result.put(String.valueOf(d.get("_id")), ((Date) d.get("last")).toInstant());
        }
        return result;
    }

    // ---- helpers ----

    private List<Document> aggregate(List<Document> pipeline) {
        MongoCollection<Document> c = mongo.getCollection(UsageEventDocument.COLLECTION);
        return c.aggregate(pipeline).into(new ArrayList<>());
    }

    private static Document first(List<Document> docs) {
        return docs.isEmpty() ? new Document() : docs.get(0);
    }

    private static long lng(Object o) {
        return o instanceof Number n ? n.longValue() : 0;
    }

    private static double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    static UsageEventView view(UsageEventDocument d) {
        return new UsageEventView(d.timestamp(), d.requestId(), d.userId(), d.model(), d.promptTokens(),
                d.completionTokens(), d.totalTokens(), d.latencyMs(), d.status(), d.errorCode(), d.cost(),
                d.currency(), d.stream(), d.usageSource());
    }
}
