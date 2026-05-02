package configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Component
public class JwtGatewayFilter implements WebFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "";

        if ("OPTIONS".equalsIgnoreCase(method)) {
            // Short-circuit preflight so downstream services (and their CORS/security configs)
            // cannot accidentally cause 403/401 and missing CORS headers.
            addCorsHeaders(exchange, true);

            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        // Unauthenticated file access (portal attachment links, direct URLs to insurance-service uploads).
        if (path.startsWith("/api/files")) {
            return chain.filter(exchange);
        }

        if (path.startsWith("/uploads")) {
            return chain.filter(exchange);
        }


        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            addCorsHeaders(exchange, false);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

            String userId = extractUserId(claims);
            if (StringUtils.hasText(userId)) {
                requestBuilder.header("X-User-Id", userId);
                requestBuilder.header("userId", userId);
            }

            String role = extractRole(claims);
            if (StringUtils.hasText(role)) {
                requestBuilder.header("role", role);
            }

            ServerHttpRequest newRequest = requestBuilder.build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (JwtException | IllegalArgumentException e) {
            addCorsHeaders(exchange, false);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private void addCorsHeaders(ServerWebExchange exchange, boolean preflight) {
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (!StringUtils.hasText(origin)) {
            return;
        }

        String reqMethod = exchange.getRequest().getHeaders().getFirst("Access-Control-Request-Method");
        String reqHeaders = exchange.getRequest().getHeaders().getFirst("Access-Control-Request-Headers");

        exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        exchange.getResponse().getHeaders().set(HttpHeaders.VARY, "Origin");
        exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        if (preflight) {
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    StringUtils.hasText(reqMethod) ? reqMethod : "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            exchange.getResponse().getHeaders().set(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    StringUtils.hasText(reqHeaders)
                            ? reqHeaders
                            : "Authorization,Content-Type,Accept,Origin,X-Requested-With");
        }
    }

    private String extractUserId(Claims claims) {
        Object uid = claims.get("userId");
        if (uid instanceof Number number) {
            return String.valueOf(number.longValue());
        }
        if (uid instanceof String str && StringUtils.hasText(str)) {
            return str;
        }
        return null;
    }

    private String extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (StringUtils.hasText(role)) {
            return role.startsWith("ROLE_") ? role.substring(5) : role;
        }

        String roles = claims.get("roles", String.class);
        if (StringUtils.hasText(roles)) {
            String first = roles.contains(",") ? roles.split(",")[0].trim() : roles.trim();
            return first.startsWith("ROLE_") ? first.substring(5) : first;
        }
        return null;
    }
}
