package ru.inno.itpd.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.security.SecurityErrorHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {
    private static final String ADMIN_ROLE = "gateway-admin";

    private final SecurityErrorHandler errors;
    private final GatewayProperties props;

    public SecurityConfig(SecurityErrorHandler errors, GatewayProperties props) {
        this.errors = errors;
        this.props = props;
    }

    /** /admin/**: JWT + роль gateway-admin (401 без токена, 403 без роли) + CORS для админки. */
    @Bean
    @Order(1)
    SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .cors(cors -> cors.configurationSource(adminCors()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ADMIN_ROLE))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoles()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors));
        return http.build();
    }

    /** /chat/** и /models: достаточно валидного JWT (пользовательского или service account). */
    @Bean
    @Order(2)
    SecurityFilterChain clientFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/chat/**", "/models", "/models/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoles()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors));
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    private CorsConfigurationSource adminCors() {
        var source = new UrlBasedCorsConfigurationSource();
        List<String> origins = props.admin().corsOrigins();
        if (!origins.isEmpty()) {
            var cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(origins);
            cfg.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                    HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
            cfg.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
            cfg.setExposedHeaders(List.of("X-Request-ID", "X-Restart-Required", HttpHeaders.CONTENT_DISPOSITION));
            cfg.setAllowCredentials(false);   // токен передаётся заголовком, куки не нужны
            cfg.setMaxAge(3600L);
            source.registerCorsConfiguration("/admin/**", cfg);
        }
        return source;
    }

    /** Keycloak puts realm roles into the realm_access.roles claim, not into scopes. */
    private JwtAuthenticationConverter keycloakRoles() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.<GrantedAuthority>of();
            }
            return roles.stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return converter;
    }
}
