package ru.inno.itpd.gateway.core.service;

import java.io.IOException;
import java.io.OutputStream;

/** Подготовленный SSE-ответ: все проверки и вызов апстрима уже позади, остаётся перекачать поток. */
@FunctionalInterface
public interface StreamSession {
    void writeTo(OutputStream out) throws IOException;
}
