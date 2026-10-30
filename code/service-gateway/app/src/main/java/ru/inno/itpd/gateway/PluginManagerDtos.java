package ru.inno.itpd.gateway;

import ru.inno.itpd.gateway.PluginDtos.PluginInfo;

import java.util.List;

public final class PluginManagerDtos {
    private PluginManagerDtos() {}

    public record RequestFiltersInput(ChatRequest request, List<PluginInfo> plugins) {}

    public record ResponseFiltersInput(ChatRequest request, ChatCompletionResponse response,
                                       List<PluginInfo> plugins) {}
}