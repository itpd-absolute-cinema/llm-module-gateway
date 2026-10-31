package ru.inno.itpd.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {
    private final GatewayProperties props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        res.setHeader("X-Request-ID", UUID.randomUUID().toString());

        String key = props.adminApiKey();
        if (key == null || key.isBlank()) {
            throw new GatewayException(HttpStatus.SERVICE_UNAVAILABLE, "admin_disabled",
                    "Plugin administration is disabled because GATEWAY_ADMIN_API_KEY is unset");
        }
        String header = req.getHeader("Authorization");
        boolean ok = header != null && header.startsWith("Bearer ")
                && MessageDigest.isEqual(
                        header.substring(7).getBytes(StandardCharsets.UTF_8),
                        key.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            res.setHeader("WWW-Authenticate", "Bearer");
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "unauthorized",
                    "Missing or invalid admin bearer credentials");
        }
        return true;
    }
}