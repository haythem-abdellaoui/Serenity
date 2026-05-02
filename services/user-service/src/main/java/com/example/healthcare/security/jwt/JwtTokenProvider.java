package com.example.healthcare.security.jwt;

import com.example.healthcare.security.userdetails.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return buildToken(details.getId(), details.getEmail(), roles);
        }
        throw new IllegalStateException("JWT requires CustomUserDetails principal, got: "
                + (principal != null ? principal.getClass().getName() : "null"));
    }

    /**
     * OAuth and other flows where the principal is not a {@code CustomUserDetails}.
     */
    public String generateToken(String email, String roles, Long userId) {
        return buildToken(userId, email, roles);
    }

    private String buildToken(Long userId, String email, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        String primaryRole = "";
        if (roles != null && !roles.isEmpty()) {
            int comma = roles.indexOf(',');
            primaryRole = comma >= 0 ? roles.substring(0, comma).trim() : roles.trim();
        }

        var builder = Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .claim("role", primaryRole)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
