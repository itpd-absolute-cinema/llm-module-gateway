package ru.inno.itpd.gateway.api.dto;

import java.util.List;

/** Постраничный ответ; page — с нуля. */
public record PageResponse<T>(List<T> items, int page, int size, long total) {
}
