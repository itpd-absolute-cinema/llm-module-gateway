package ru.inno.itpd.gateway.core.client.llm;

import java.io.IOException;
import java.io.InputStream;

/** Открытый SSE-поток от апстрима; закрытие отменяет генерацию на стороне апстрима. */
public record LlmStream(InputStream body) implements AutoCloseable {
    @Override
    public void close() {
        try {
            body.close();
        } catch (IOException ignored) {
            // nothing useful to do
        }
    }
}
