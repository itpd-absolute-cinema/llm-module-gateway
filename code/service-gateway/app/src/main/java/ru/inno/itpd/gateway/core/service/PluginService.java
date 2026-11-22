package ru.inno.itpd.gateway.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.PluginDtos.PluginInfo;
import ru.inno.itpd.gateway.api.dto.PluginDtos.PluginView;
import ru.inno.itpd.gateway.api.dto.PluginType;
import ru.inno.itpd.gateway.core.dao.entity.PluginDocument;
import ru.inno.itpd.gateway.core.dao.repository.PluginRepository;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.core.file.PluginFileStorage;
import ru.inno.itpd.gateway.core.service.PluginMetadataService.PluginMetadata;
import ru.inno.itpd.gateway.util.PluginUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PluginService {
    /** Версия набора метаданных: 1 — description + options_schema, 2 — + metrics. Старые документы достраиваются лениво. */
    private static final int METADATA_VERSION = 2;

    private final PluginRepository repo;
    private final PluginFileStorage files;
    private final PluginMetadataService metadata;
    private final AuditService audit;
    private final JsonMapper mapper;

    public PluginService(PluginRepository repo, PluginFileStorage files, PluginMetadataService metadata,
                         AuditService audit, JsonMapper mapper) {
        this.repo = repo;
        this.files = files;
        this.metadata = metadata;
        this.audit = audit;
        this.mapper = mapper;
    }

    public synchronized List<PluginView> list() {
        return repo.findAllByOrderByPositionAsc().stream().map(this::view).toList();
    }

    public synchronized PluginView get(String name) {
        return view(find(repo.findAllByOrderByPositionAsc(), name));
    }

    public synchronized PluginView create(String name, PluginType type, Map<String, Object> options,
                                          String before, String after, byte[] source, PluginMetadata meta) {
        List<PluginDocument> all = new ArrayList<>(repo.findAllByOrderByPositionAsc());
        if (all.stream().anyMatch(d -> d.getName().equals(name))) {
            throw new GatewayException(HttpStatus.CONFLICT, "plugin_exists", "Plugin already exists");
        }
        PluginDocument d = new PluginDocument(name, type, false, options, all.size());
        d.setDescription(meta.description());
        d.setOptionsSchemaJson(meta.optionsSchemaJson());
        d.setMetricsJson(meta.metricsJson());
        d.setMetadataVersion(METADATA_VERSION);
        all.add(d);
        place(all, d, before, after);
        files.save(name, source);
        persist(all);
        PluginView result = view(d);
        audit.record("plugin.create", "plugin", name, null, result);
        return result;
    }

    public synchronized PluginView updateSettings(String name, Map<String, Object> options,
                                                  String before, String after) {
        List<PluginDocument> all = new ArrayList<>(repo.findAllByOrderByPositionAsc());
        PluginDocument d = find(all, name);
        PluginView old = view(d);
        place(all, d, before, after);
        d.setOptions(options);
        persist(all);
        PluginView result = view(d);
        audit.record("plugin.update", "plugin", name, old, result);
        return result;
    }

    public synchronized PluginView setStatus(String name, Boolean enabled, String before, String after) {
        List<PluginDocument> all = new ArrayList<>(repo.findAllByOrderByPositionAsc());
        PluginDocument d = find(all, name);
        PluginView old = view(d);
        place(all, d, before, after);
        if (enabled != null) d.setEnabled(enabled);
        persist(all);
        PluginView result = view(d);
        audit.record("plugin.status", "plugin", name, old, result);
        return result;
    }

    public synchronized void delete(String name) {
        PluginDocument d = find(repo.findAllByOrderByPositionAsc(), name);
        if (d.isEnabled()) {
            throw new GatewayException(HttpStatus.CONFLICT, "plugin_enabled", "Plugin is enabled");
        }
        PluginView old = view(d);
        repo.delete(d);
        files.delete(name);
        audit.record("plugin.delete", "plugin", name, old, null);
    }

    /** Enabled plugins in chain order, with their settings. */
    public List<PluginInfo> enabledChain() {
        return repo.findAllByOrderByPositionAsc().stream()
                .filter(PluginDocument::isEnabled)
                .map(this::info)
                .toList();
    }

    public List<PluginInfo> enabledChain(PluginType type) {
        return repo.findAllByOrderByPositionAsc().stream()
                .filter(d -> d.isEnabled() && d.getType() == type)
                .map(this::info)
                .toList();
    }

    // ---- internals ----

    private PluginDocument find(List<PluginDocument> all, String name) {
        PluginUtils.requireValidName(name);
        return all.stream().filter(d -> d.getName().equals(name)).findFirst()
                .orElseThrow(() -> new GatewayException(HttpStatus.NOT_FOUND, "plugin_not_found", "Plugin not found"));
    }

    /** Переставляет d относительно before/after; без них ничего не делает. */
    private void place(List<PluginDocument> all, PluginDocument d, String before, String after) {
        if (before != null && after != null) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_ordering",
                    "'before' and 'after' are mutually exclusive");
        }
        String targetName = before != null ? before : after;
        if (targetName == null) return;
        PluginUtils.requireValidName(targetName);
        PluginDocument target = all.stream().filter(x -> x.getName().equals(targetName)).findFirst()
                .orElseThrow(() -> new GatewayException(HttpStatus.NOT_FOUND, "ordering_target_not_found",
                        "Ordering target has no configuration entry"));
        if (target == d) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_ordering",
                    "Cannot order a plugin relative to itself");
        }
        all.remove(d);
        int idx = all.indexOf(target);
        all.add(before != null ? idx : idx + 1, d);
    }

    private void persist(List<PluginDocument> all) {
        for (int i = 0; i < all.size(); i++) all.get(i).setPosition(i);
        repo.saveAll(all);
    }

    /** То, что уходит в Plugin Manager: без метаданных. */
    private PluginInfo info(PluginDocument d) {
        return new PluginInfo(d.getName(), d.getType(), d.isEnabled(), d.getOptions());
    }

    /** Представление для админ-API. Плагины, загруженные до появления метаданных, дозаполняются лениво. */
    private PluginView view(PluginDocument d) {
        if (d.getMetadataVersion() < METADATA_VERSION) backfill(d);
        return new PluginView(d.getName(), d.getType(), d.isEnabled(), d.getOptions(), d.getPosition(),
                d.getDescription(), parseJson(d.getOptionsSchemaJson()), parseJson(d.getMetricsJson()));
    }

    private void backfill(PluginDocument d) {
        byte[] source = files.read(d.getName());
        if (source == null) return;   // файла нет — попробуем в следующий раз
        PluginMetadata meta = metadata.extractLenient(source);
        d.setDescription(meta.description());
        d.setOptionsSchemaJson(meta.optionsSchemaJson());
        d.setMetricsJson(meta.metricsJson());
        d.setMetadataVersion(METADATA_VERSION);
        repo.save(d);
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException e) {
            return null;
        }
    }
}
