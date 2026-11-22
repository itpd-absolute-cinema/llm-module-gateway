package ru.inno.itpd.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties("gateway")
public record GatewayProperties(
        String adminApiKey,
        Path pluginsDir,
        String pluginManagerUrl,
        Duration pluginManagerTimeout,
        LlmProperties llm,
        @DefaultValue AdminProperties admin,
        @DefaultValue KeycloakProperties keycloak,
        @DefaultValue ModelsProperties models,
        @DefaultValue StreamingProperties streaming,
        @DefaultValue BillingProperties billing
) {

    public record LlmProperties(String provider, String baseUrl) {
    }

    /** Origins веб-админки, которым разрешён CORS на /admin/**. Пусто = CORS выключен. */
    public record AdminProperties(List<String> corsOrigins) {
        public AdminProperties {
            corsOrigins = corsOrigins == null ? List.of() : corsOrigins.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList();
        }
    }

    /** Доступ к Keycloak Admin REST через service account шлюза (client credentials). */
    public record KeycloakProperties(
            String baseUrl,
            @DefaultValue("gateway") String realm,
            String adminClientId,
            String adminClientSecret,
            @DefaultValue("true") boolean syncEnabled
    ) {
        public boolean adminConfigured() {
            return notBlank(baseUrl) && notBlank(adminClientId) && notBlank(adminClientSecret);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    public record ModelsProperties(@DefaultValue("30s") Duration cacheTtl) {
    }

    public record StreamingProperties(
            @DefaultValue("true") boolean rejectWithResponseFilters,
            @DefaultValue("10m") Duration timeout
    ) {
    }

    public record BillingProperties(@DefaultValue("USD") String defaultCurrency) {
    }
}
