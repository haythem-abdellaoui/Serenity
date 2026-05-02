package com.serenity.monitoring.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {

    private static final String JWT_SECRET_PROPERTY = "app.jwt.secret";
    private final SecretKey signingKey;

    public JwtTokenProvider(Environment environment) {
        String jwtSecret = environment.getProperty(JWT_SECRET_PROPERTY);
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Missing JWT secret: " + JWT_SECRET_PROPERTY);
        }

        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

