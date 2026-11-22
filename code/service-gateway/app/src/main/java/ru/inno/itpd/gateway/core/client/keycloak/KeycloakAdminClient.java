package ru.inno.itpd.gateway.core.client.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Keycloak Admin REST от имени service account шлюза (client credentials).
 * Нужные роли service account (клиент realm-management): view-users, manage-users
 * (создание пользователей и выдача realm-роли — manage-users; чтение роли — view-users).
 */
@Component
public class KeycloakAdminClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KcUser(String id, String username, String email, String firstName, String lastName, Boolean enabled) {
    }

    /** Данные нового пользователя. Пароль не должен попадать в логи, поэтому toString его маскирует. */
    public record NewUser(String username, String email, String firstName, String lastName,
                          String password, boolean temporaryPassword) {
        @Override
        public String toString() {
            return "NewUser[username=" + username + ", email=" + email + ", password=***]";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RoleRepresentation(String id, String name) {
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CountResponse(long count) {
    }

    private final GatewayProperties.KeycloakProperties props;
    private final RestClient http;

    private String token;
    private Instant tokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(GatewayProperties gatewayProps) {
        this.props = gatewayProps.keycloak();
        this.http = props.adminConfigured()
                ? RestClient.builder().baseUrl(trimSlash(props.baseUrl())).build()
                : null;
    }

    public boolean isConfigured() {
        return http != null;
    }

    public List<KcUser> search(String search, int first, int max) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("realm", props.realm());
        vars.put("first", first);
        vars.put("max", max);
        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) vars.put("search", search.trim());
        KcUser[] users = call(() -> http.get().uri(b -> {
            var u = b.path("/admin/realms/{realm}/users")
                    .queryParam("first", "{first}").queryParam("max", "{max}")
                    .queryParam("briefRepresentation", "true");
            if (hasSearch) u = u.queryParam("search", "{search}");
            return u.build(vars);
        }).headers(h -> h.setBearerAuth(token())).retrieve().body(KcUser[].class));
        return users == null ? List.of() : List.of(users);
    }

    public long count(String search) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("realm", props.realm());
        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) vars.put("search", search.trim());
        Long n = call(() -> http.get().uri(b -> {
            var u = b.path("/admin/realms/{realm}/users/count");
            if (hasSearch) u = u.queryParam("search", "{search}");
            return u.build(vars);
        }).headers(h -> h.setBearerAuth(token())).retrieve().body(Long.class));
        return n == null ? 0 : n;
    }

    public Optional<KcUser> find(String id) {
        try {
            KcUser user = call(() -> http.get().uri("/admin/realms/{realm}/users/{id}", props.realm(), id)
                    .headers(h -> h.setBearerAuth(token())).retrieve().body(KcUser.class));
            return Optional.ofNullable(user);
        } catch (GatewayException e) {
            if ("user_not_found".equals(e.code())) return Optional.empty();
            throw e;
        }
    }

    public void setEnabled(String id, boolean enabled) {
        call(() -> {
            http.put().uri("/admin/realms/{realm}/users/{id}", props.realm(), id)
                    .headers(h -> h.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", enabled))
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    /**
     * Создаёт включённого пользователя с подтверждённым email и паролем; возвращает его id.
     * 409 → user_exists, отказ политики паролей → 422 weak_password, недоступность/нехватка прав → 502.
     */
    public String createUser(NewUser user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", user.username());
        if (user.email() != null) body.put("email", user.email());
        if (user.firstName() != null) body.put("firstName", user.firstName());
        if (user.lastName() != null) body.put("lastName", user.lastName());
        body.put("enabled", true);
        body.put("emailVerified", true);
        body.put("credentials", List.of(Map.of(
                "type", "password", "value", user.password(), "temporary", user.temporaryPassword())));

        var response = write(() -> http.post().uri("/admin/realms/{realm}/users", props.realm())
                        .headers(h -> h.setBearerAuth(token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve().toBodilessEntity(),
                KeycloakAdminClient::mapCreateError);
        var location = response.getHeaders().getLocation();
        String path = location == null ? "" : location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        if (id.isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                    "Keycloak created the user but did not return its id");
        }
        return id;
    }

    /** Выдаёт пользователю realm-роль по имени. */
    public void assignRealmRole(String userId, String roleName) {
        RoleRepresentation role = write(() -> http.get()
                        .uri("/admin/realms/{realm}/roles/{role}", props.realm(), roleName)
                        .headers(h -> h.setBearerAuth(token()))
                        .retrieve().body(RoleRepresentation.class),
                e -> e.getStatusCode().value() == 404
                        ? new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                        "Realm role '" + roleName + "' does not exist in Keycloak")
                        : mapStatus(e, "reading the realm role '" + roleName + "'"));
        write(() -> http.post()
                        .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", props.realm(), userId)
                        .headers(h -> h.setBearerAuth(token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(Map.of("id", role.id(), "name", role.name())))
                        .retrieve().toBodilessEntity(),
                e -> mapStatus(e, "assigning the realm role '" + roleName + "'"));
    }

    public void deleteUser(String id) {
        write(() -> http.delete().uri("/admin/realms/{realm}/users/{id}", props.realm(), id)
                        .headers(h -> h.setBearerAuth(token()))
                        .retrieve().toBodilessEntity(),
                e -> mapStatus(e, "deleting the user"));
    }

    // ---- internals ----

    @FunctionalInterface
    private interface Call<T> {
        T run();
    }

    /** Чтение: ошибки Keycloak → GatewayException (404 → user_not_found, недоступен → 503). */
    private <T> T call(Call<T> call) {
        return call(call, e -> e.getStatusCode().value() == 404
                ? new GatewayException(HttpStatus.NOT_FOUND, "user_not_found", "User not found in Keycloak")
                : mapStatus(e, null));
    }

    /** Запись: то же, но «Keycloak недоступен» — 502 (шлюз не смог выполнить действие у вышестоящего сервиса). */
    private <T> T write(Call<T> call, Function<RestClientResponseException, GatewayException> mapper) {
        try {
            return call(call, mapper);
        } catch (GatewayException e) {
            if (e.status() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new GatewayException(HttpStatus.BAD_GATEWAY, e.code(), e.getMessage());
            }
            throw e;
        }
    }

    /** Выполняет вызов, один раз перевыпускает токен при 401, маппит ошибки в GatewayException. */
    private <T> T call(Call<T> call, Function<RestClientResponseException, GatewayException> mapper) {
        try {
            try {
                return call.run();
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != 401) throw e;
                invalidateToken();
                return call.run();
            }
        } catch (RestClientResponseException e) {
            throw mapper.apply(e);
        } catch (ResourceAccessException e) {
            throw new GatewayException(HttpStatus.SERVICE_UNAVAILABLE, "identity_provider_unavailable",
                    "Could not connect to Keycloak");
        }
    }

    private static GatewayException mapStatus(RestClientResponseException e, String action) {
        int status = e.getStatusCode().value();
        String what = action == null ? "" : " while " + action;
        if (status == 401 || status == 403) {
            return new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                    "Keycloak denied access" + what + " (status " + status + "): the gateway service account "
                            + "lacks permissions; it needs the realm-management roles view-users and manage-users");
        }
        return new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                "Keycloak responded with status " + status + what);
    }

    /** Ошибки POST /users: 409 — дубликат, 400 — политика паролей (422) или другие невалидные данные. */
    private static GatewayException mapCreateError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String reason = keycloakReason(e);
        if (status == 409) {
            return new GatewayException(HttpStatus.CONFLICT, "user_exists",
                    reason != null ? reason : "A user with this username or email already exists");
        }
        if (status == 400) {
            if (reason != null && reason.toLowerCase().contains("password")) {
                return new GatewayException(HttpStatus.UNPROCESSABLE_ENTITY, "weak_password",
                        "Password rejected by the realm policy: " + reason);
            }
            return new GatewayException(HttpStatus.BAD_REQUEST, "invalid_user",
                    reason != null ? reason : "Keycloak rejected the user data");
        }
        return mapStatus(e, "creating the user");
    }

    /** Человекочитаемая причина из тела ответа Keycloak (errorMessage / error_description / error) или null. */
    private static String keycloakReason(RestClientResponseException e) {
        try {
            JsonNode n = JSON.readTree(e.getResponseBodyAsString());
            for (String key : List.of("errorMessage", "error_description", "error")) {
                String v = n.path(key).asString("");
                if (!v.isBlank()) return v;
            }
        } catch (RuntimeException ignored) {
            // тело не JSON — причины нет
        }
        return null;
    }

    private synchronized String token() {
        if (token != null && Instant.now().isBefore(tokenExpiry)) return token;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.adminClientId());
        form.add("client_secret", props.adminClientSecret());
        try {
            TokenResponse r = http.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", props.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (r == null || r.accessToken() == null) {
                throw new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                        "Keycloak returned no access token for the gateway service account");
            }
            token = r.accessToken();
            tokenExpiry = Instant.now().plus(Duration.ofSeconds(Math.max(r.expiresIn() - 30, 5)));
            return token;
        } catch (RestClientResponseException e) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                    "Keycloak rejected the gateway service account credentials (status "
                            + e.getStatusCode().value() + ")");
        }
    }

    private synchronized void invalidateToken() {
        token = null;
        tokenExpiry = Instant.EPOCH;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
