package ru.inno.itpd.gateway.util;

import ru.inno.itpd.gateway.core.exception.GatewayException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Разбор и проверка query-параметров админ-API. */
public final class QueryParams {
    public static final int MAX_PAGE_SIZE = 200;

    private QueryParams() {
    }

    /** ISO-8601 datetime (с зоной) или дата YYYY-MM-DD (= 00:00:00Z). */
    public static Instant instant(String name, String value, Instant defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        String v = value.trim();
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException ignored) {
            // try the next format
        }
        try {
            return OffsetDateTime.parse(v).toInstant();
        } catch (DateTimeParseException ignored) {
            // try the next format
        }
        try {
            return LocalDate.parse(v).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw invalid(name, "must be an ISO-8601 date-time (e.g. 2026-11-01T00:00:00Z) or a date (2026-11-01)");
        }
    }

    public static void requireRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_range", "'from' must be earlier than 'to'");
        }
    }

    public static void page(int page, int size) {
        if (page < 0) throw invalid("page", "must be >= 0");
        if (size < 1 || size > MAX_PAGE_SIZE) throw invalid("size", "must be between 1 and " + MAX_PAGE_SIZE);
    }

    public static String oneOf(String name, String value, Set<String> allowed) {
        if (value == null || value.isBlank()) return null;
        if (!allowed.contains(value)) throw invalid(name, "must be one of " + allowed);
        return value;
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static GatewayException invalid(String name, String why) {
        return new GatewayException(HttpStatus.BAD_REQUEST, "invalid_parameter", "Parameter '" + name + "' " + why);
    }
}
