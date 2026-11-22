package ru.inno.itpd.gateway.cucumber.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Replaces ONLY the JwtDecoder (the part that talks to Keycloak).
 * Everything else is the real production security: SecurityFilterChain,
 * the realm_access.roles converter, hasRole(...), 401/403 handlers.
 * Lives in src/test, so it can never end up in the production jar.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfiguration {

    public static final String ADMIN_TOKEN = "test-token";                    // sub admin-1, realm role gateway-admin
    public static final String USER_TOKEN = "test-user-token";                // sub user-1, no admin role
    public static final String OTHER_USER_TOKEN = "test-other-user-token";    // sub user-2, no admin role

    public static final String ADMIN_ID = "admin-1";
    public static final String USER_ID = "user-1";
    public static final String OTHER_USER_ID = "user-2";

    private record Identity(String sub, String username, List<String> roles) {
    }

    private static final Map<String, Identity> IDENTITIES = Map.of(
            ADMIN_TOKEN, new Identity(ADMIN_ID, "admin", List.of("gateway-admin")),
            USER_TOKEN, new Identity(USER_ID, "alice", List.of("gateway-user")),
            OTHER_USER_TOKEN, new Identity(OTHER_USER_ID, "bob", List.of("gateway-user")));

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            Identity who = IDENTITIES.get(token);
            if (who == null) {
                throw new BadJwtException("Unknown test token");   // -> 401 via SecurityErrorHandler
            }
            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(who.sub())
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .claim("preferred_username", who.username())
                    .claim("realm_access", Map.of("roles", who.roles()))  // same shape as real Keycloak
                    .build();
        };
    }
}
