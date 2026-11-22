package ru.inno.itpd.gateway.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    /** Идентификатор текущего запроса (тот же, что в заголовке X-Request-ID); null вне HTTP-запроса. */
    public static String current() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        Object v = attrs.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return v == null ? null : v.toString();
    }

    public static String of(HttpServletRequest request) {
        Object v = request.getAttribute(ATTRIBUTE);
        return v == null ? UUID.randomUUID().toString() : v.toString();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        req.setAttribute(ATTRIBUTE, id);
        res.setHeader("X-Request-ID", id);
        MDC.put("request_id", id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("request_id");
        }
    }
}
