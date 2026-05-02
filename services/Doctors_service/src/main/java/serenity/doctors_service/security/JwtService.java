package serenity.doctors_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        // Decode the Hex/Base64 string properly
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        // If your property is a raw Hex string, use: Decoders.HEX.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // New method to extract roles for Spring Security
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");

        if (rolesObj instanceof List<?> rolesList) {
            // Handle case where roles is already a list
            return rolesList.stream()
                    .map(Object::toString)
                    .toList();
        } else if (rolesObj instanceof String rolesStr) {
            // Handle case where roles is a single comma-separated string
            return List.of(rolesStr.split(","));
        }

        return List.of();
    }

    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Number n) {
            return n.longValue();
        }
        if (userId instanceof String s && !s.isBlank()) {
            return Long.parseLong(s.trim());
        }
        return null;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}