package ru.inno.itpd.gateway.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Администратор, выполняющий текущий запрос: sub и preferred_username из JWT. */
public record Actor(String id, String username) {

    public static Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            return new Actor(jwt.getSubject(), jwt.getClaimAsString("preferred_username"));
        }
        return new Actor(auth == null ? "unknown" : auth.getName(), null);
    }
}
