package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.inno.itpd.gateway.api.dto.PageResponse;
import ru.inno.itpd.gateway.api.dto.UsageDtos.Timeseries;
import ru.inno.itpd.gateway.api.dto.UsageDtos.UsageEventView;
import ru.inno.itpd.gateway.api.dto.UsageDtos.UsageSummary;
import ru.inno.itpd.gateway.core.service.UsageQueryService;
import ru.inno.itpd.gateway.core.service.UsageQueryService.Filter;
import ru.inno.itpd.gateway.util.CsvWriter;
import ru.inno.itpd.gateway.util.QueryParams;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/usage")
@Tag(name = "Admin: usage")
@SecurityRequirement(name = "HTTPBearer")
public class UsageAdminController {
    private static final Set<String> STATUSES = Set.of("ok", "error", "blocked", "rejected_by_plugin");
    private static final Set<String> INTERVALS = Set.of("hour", "day");
    private static final Set<String> GROUPS = Set.of("user", "model");
    private static final String[] CSV_HEADER = {"timestamp", "request_id", "user_id", "model", "status", "error_code",
            "prompt_tokens", "completion_tokens", "total_tokens", "latency_ms", "cost", "currency", "stream", "usage_source"};

    private final UsageQueryService usage;

    public UsageAdminController(UsageQueryService usage) {
        this.usage = usage;
    }

    @GetMapping("/summary")
    @Operation(summary = "Usage Summary",
            description = "Totals for [from, to). Defaults: to = now, from = to - 30 days. error_rate counts every status except ok.",
            operationId = "admin_usage_summary")
    public UsageSummary summary(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
                                @RequestParam(name = "user_id", required = false) String userId,
                                @RequestParam(required = false) String model) {
        return usage.summary(filter(from, to, userId, model, null));
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Usage Time Series", description = "Buckets by hour or day (UTC), optionally grouped by user or model.",
            operationId = "admin_usage_timeseries")
    public Timeseries timeseries(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
                                 @RequestParam(defaultValue = "day") String interval,
                                 @RequestParam(name = "group_by", required = false) String groupBy,
                                 @RequestParam(name = "user_id", required = false) String userId,
                                 @RequestParam(required = false) String model) {
        QueryParams.oneOf("interval", interval, INTERVALS);
        String group = QueryParams.oneOf("group_by", groupBy, GROUPS);
        return usage.timeseries(filter(from, to, userId, model, null), interval, group);
    }

    @GetMapping("/events")
    @Operation(summary = "Usage Events", description = "Newest first.", operationId = "admin_usage_events")
    public PageResponse<UsageEventView> events(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(name = "user_id", required = false) String userId,
                                               @RequestParam(required = false) String model,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        QueryParams.page(page, size);
        return usage.events(filter(from, to, userId, model, status), page, size);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @Operation(summary = "Export Usage Events As CSV", description = "Same filters as /admin/usage/events; oldest first, streamed.",
            operationId = "admin_usage_export")
    public ResponseEntity<StreamingResponseBody> export(@RequestParam(required = false) String from,
                                                        @RequestParam(required = false) String to,
                                                        @RequestParam(name = "user_id", required = false) String userId,
                                                        @RequestParam(required = false) String model,
                                                        @RequestParam(required = false) String status) {
        Filter f = filter(from, to, userId, model, status);
        StreamingResponseBody body = out -> {
            Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            CsvWriter.row(w, CSV_HEADER);
            try (Stream<UsageEventView> events = usage.stream(f)) {
                var it = events.iterator();
                while (it.hasNext()) {
                    UsageEventView e = it.next();
                    CsvWriter.row(w, String.valueOf(e.timestamp()), e.requestId(), e.userId(), e.model(), e.status(),
                            e.errorCode(), String.valueOf(e.promptTokens()), String.valueOf(e.completionTokens()),
                            String.valueOf(e.totalTokens()), String.valueOf(e.latencyMs()),
                            e.cost() == null ? "" : String.valueOf(e.cost()), e.currency(),
                            String.valueOf(e.stream()), e.usageSource());
                }
            }
            w.flush();
        };
        String file = "usage-" + f.from().toString().replace(':', '-') + "_" + f.to().toString().replace(':', '-') + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(body);
    }

    private static Filter filter(String from, String to, String userId, String model, String status) {
        Instant end = QueryParams.instant("to", to, Instant.now());
        Instant start = QueryParams.instant("from", from, end.minus(Duration.ofDays(30)));
        QueryParams.requireRange(start, end);
        return new Filter(start, end, QueryParams.blankToNull(userId), QueryParams.blankToNull(model),
                QueryParams.oneOf("status", status, STATUSES));
    }
}
