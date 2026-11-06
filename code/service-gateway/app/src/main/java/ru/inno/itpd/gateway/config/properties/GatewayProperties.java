package ru.inno.itpd.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("gateway")
public record GatewayProperties(String adminApiKey, Path pluginsDir,
                                String pluginManagerUrl, Duration pluginManagerTimeout) {}