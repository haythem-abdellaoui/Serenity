package com.example.pharmacy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final UserIdentityResolver userIdentityResolver;

    @Value("${app.security.allow-unsafe-header-auth:false}")
    private boolean allowUnsafeHeaderAuth;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String userId = null;
        String role = null;

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        UserIdentityResolver.ResolvedIdentity identity = userIdentityResolver.resolveFromBearer(authHeader);
        if (identity != null) {
            userId = identity.userId();
            role = identity.role();
        } else if (allowUnsafeHeaderAuth) {
            userId = request.getHeader("userId");
            role = request.getHeader("role");
        }

        if (!isBlankOrNull(userId) && !isBlankOrNull(role)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(authority))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlankOrNull(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value);
    }
}
