package ru.inno.itpd.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Имя плагина уже проверено по regexp, поэтому path traversal невозможен. */
@Component
@RequiredArgsConstructor
public class PluginFileStorage {
    private final GatewayProperties props;

    public void save(String name, byte[] source) {
        try {
            Files.createDirectories(props.pluginsDir());
            Files.write(file(name), source);
        } catch (IOException e) {
            throw storageFailed();
        }
    }

    public void delete(String name) {
        try {
            Files.deleteIfExists(file(name));
        } catch (IOException e) {
            throw storageFailed();
        }
    }

    private Path file(String name) {
        return props.pluginsDir().resolve(name + ".py");
    }

    private GatewayException storageFailed() {
        return new GatewayException(HttpStatus.INTERNAL_SERVER_ERROR, "storage_failed",
                "Plugin storage operation failed");
    }
}