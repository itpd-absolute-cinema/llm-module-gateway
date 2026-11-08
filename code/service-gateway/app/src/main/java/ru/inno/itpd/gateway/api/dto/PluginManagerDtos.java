package ru.inno.itpd.gateway.api.dto;

import ru.inno.itpd.gateway.api.dto.PluginDtos.PluginInfo;

import java.util.List;

public final class PluginManagerDtos {
    private PluginManagerDtos() {}

    public record RequestFiltersInput(ChatRequest request, List<PluginInfo> plugins) {}

    public record ResponseFiltersInput(ChatRequest request, ChatCompletionResponse response,
                                       List<PluginInfo> plugins) {}
}