package ru.inno.itpd.gateway.core.file;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.exception.GatewayException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Имя плагина уже проверено по regexp, поэтому path traversal невозможен. */
@Component
public class PluginFileStorage {
    private final GatewayProperties props;

    public PluginFileStorage(GatewayProperties props) {
        this.props = props;
    }

    /** Исходник плагина или null, если файла нет. */
    public byte[] read(String name) {
        try {
            Path f = file(name);
            return Files.exists(f) ? Files.readAllBytes(f) : null;
        } catch (IOException e) {
            throw storageFailed();
        }
    }

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