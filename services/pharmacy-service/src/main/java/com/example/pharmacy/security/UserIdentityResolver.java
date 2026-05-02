package com.example.pharmacy.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserIdentityResolver {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.user-service.me-url:http://localhost:8081/api/users/me}")
    private String userMeUrl;

    public ResolvedIdentity resolveFromBearer(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(userMeUrl, HttpMethod.GET, entity, Map.class);
            Map body = response.getBody();
            if (body == null) return null;

            Object idRaw = body.get("id");
            Object roleRaw = body.get("role");
            if (idRaw == null || roleRaw == null) return null;

            String userId = String.valueOf(idRaw);
            String role = String.valueOf(roleRaw);
            return new ResolvedIdentity(userId, role);
        } catch (Exception ignored) {
            return null;
        }
    }

    public record ResolvedIdentity(String userId, String role) {}
}
