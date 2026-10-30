package ru.inno.itpd.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Проверяет синтаксис Python через `python3` (ast.parse), код не исполняется. */
@Service
public class PythonValidationService {

    public void validate(byte[] source) {
        try {
            Process p = new ProcessBuilder("python3", "-c", "import ast,sys; ast.parse(sys.stdin.buffer.read())")
                    .redirectErrorStream(true)
                    .start();
            try (OutputStream in = p.getOutputStream()) {
                in.write(source);
            }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (p.waitFor() != 0) {
                String lastLine = out.strip().lines().reduce((a, b) -> b).orElse("");
                throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_python_source",
                        "Invalid Python source: " + lastLine);
            }
        } catch (IOException e) {
            throw new GatewayException(HttpStatus.INTERNAL_SERVER_ERROR, "python_unavailable",
                    "Python interpreter is not available");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException(HttpStatus.INTERNAL_SERVER_ERROR, "validation_interrupted",
                    "Python validation was interrupted");
        }
    }
}