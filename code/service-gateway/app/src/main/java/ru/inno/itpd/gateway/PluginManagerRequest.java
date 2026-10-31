package ru.inno.itpd.gateway;

import ru.inno.itpd.gateway.PluginDtos.PluginInfo;

import java.util.List;

/** Payload for the Plugin Manager: the chat request plus the enabled plugins in chain order. */
public record PluginManagerRequest(ChatRequest request, List<PluginInfo> plugins) {}