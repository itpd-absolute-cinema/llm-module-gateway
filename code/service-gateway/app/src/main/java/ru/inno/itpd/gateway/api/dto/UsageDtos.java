package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class UsageDtos {
    private UsageDtos() {}

    public record UsageSummary(
            Instant from,
            Instant to,
            long requests,
            /** Запросы со статусом, отличным от ok (error, blocked, rejected_by_plugin). */
            long errors,
            @JsonProperty("error_rate") double errorRate,
            @JsonProperty("prompt_tokens") long promptTokens,
            @JsonProperty("completion_tokens") long completionTokens,
            @JsonProperty("total_tokens") long totalTokens,
            double cost,
            String currency,
            @JsonProperty("avg_latency_ms") double avgLatencyMs,
            @JsonProperty("by_status") Map<String, Long> byStatus
    ) {
    }

    public record TimeseriesPoint(
            Instant bucket,
            @JsonInclude(JsonInclude.Include.NON_NULL) String group,
            long requests,
            long errors,
            @JsonProperty("prompt_tokens") long promptTokens,
            @JsonProperty("completion_tokens") long completionTokens,
            @JsonProperty("total_tokens") long totalTokens,
            double cost
    ) {
    }

    public record Timeseries(
            Instant from,
            Instant to,
            String interval,
            @JsonProperty("group_by") @JsonInclude(JsonInclude.Include.NON_NULL) String groupBy,
            List<TimeseriesPoint> points
    ) {
    }

    public record UsageEventView(
            Instant timestamp,
            @JsonProperty("request_id") String requestId,
            @JsonProperty("user_id") String userId,
            String model,
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens,
            @JsonProperty("latency_ms") long latencyMs,
            String status,
            @JsonProperty("error_code") String errorCode,
            Double cost,
            String currency,
            boolean stream,
            @JsonProperty("usage_source") String usageSource
    ) {
    }
}
