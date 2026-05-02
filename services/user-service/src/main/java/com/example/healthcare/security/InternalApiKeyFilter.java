package com.example.healthcare.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protects {@code /api/internal/**} with {@code X-Internal-Key} (service-to-service calls).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.internal-api-key:}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path == null || !path.contains("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!StringUtils.hasText(internalApiKey)) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Internal API not configured");
            return;
        }
        String key = request.getHeader("X-Internal-Key");
        if (!internalApiKey.equals(key)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal API key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
