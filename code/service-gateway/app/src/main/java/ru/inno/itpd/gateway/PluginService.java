package ru.inno.itpd.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ru.inno.itpd.gateway.PluginDtos.PluginInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PluginService {
    private final PluginRepository repo;
    private final PluginFileStorage files;

    public synchronized List<PluginInfo> list() {
        return repo.findAllByOrderByPositionAsc().stream().map(this::info).toList();
    }

    public synchronized PluginInfo get(String name) {
        return info(find(repo.findAllByOrderByPositionAsc(), name));
    }

    public synchronized PluginInfo create(String name, PluginType type, Map<String, Object> options,
                                          String before, String after, byte[] source) {
        List<PluginDocument> all = new ArrayList<>(repo.findAllByOrderByPositionAsc());
        if (all.stream().anyMatch(d -> d.getName().equals(name))) {
            throw new GatewayException(HttpStatus.CONFLICT, "plugin_exists", "Plugin already exists");
        }
        PluginDocument d = new PluginDocument(name, type, false, options, all.size());
        all.add(d);
        place(all, d, before, after);
        files.save(name, source);
        persist(all);
        return info(d);
    }

    public synchronized PluginInfo updateSettings(String name, Map<String, Object> options,
                                                  String before, String after) {
        List<PluginDocument> all = new ArrayList<>(repo.findAllByOrderByPositionAsc());
        PluginDocument d = find(all, name);
        place(all, d, before, after);
        d.setOptions(options);
        persist(all);
        return info(d);
    }

    public synchronized PluginInfo setStatus(String name, Boolean enabled, String before, String after) {
        List<PluginDocument> all = new ArrayList<>(repo.findAllByOrderByPositionAsc());
        PluginDocument d = find(all, name);
        place(all, d, before, after);
        if (enabled != null) d.setEnabled(enabled);
        persist(all);
        return info(d);
    }

    public synchronized void delete(String name) {
        PluginDocument d = find(repo.findAllByOrderByPositionAsc(), name);
        if (d.isEnabled()) {
            throw new GatewayException(HttpStatus.CONFLICT, "plugin_enabled", "Plugin is enabled");
        }
        repo.delete(d);
        files.delete(name);
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

    private PluginInfo info(PluginDocument d) {
        return new PluginInfo(d.getName(), d.getType(), d.isEnabled(), d.getOptions());
    }
}