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

    public static final String ADMIN_TOKEN = "test-token";        // realm role: gateway-admin
    public static final String USER_TOKEN = "test-user-token";    // valid JWT, no admin role

    private static final Map<String, List<String>> ROLES_BY_TOKEN = Map.of(
            ADMIN_TOKEN, List.of("gateway-admin"),
            USER_TOKEN, List.of("gateway-user"));

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            List<String> roles = ROLES_BY_TOKEN.get(token);
            if (roles == null) {
                throw new BadJwtException("Unknown test token");   // -> 401 via SecurityErrorHandler
            }
            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("test-user")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .claim("realm_access", Map.of("roles", roles))  // same shape as real Keycloak
                    .build();
        };
    }
}