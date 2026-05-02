package com.example.pharmacy.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UserRoleAssignmentClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.user-service.base-url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @Value("${app.user-service.internal-api-key:}")
    private String internalApiKey;

    public void assignPharmacistRole(Long userId) {
        if (!StringUtils.hasText(internalApiKey)) {
            throw new IllegalStateException("Missing app.user-service.internal-api-key configuration");
        }

        String url = userServiceBaseUrl + "/api/internal/users/" + userId + "/role";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalApiKey);

        Map<String, String> request = Map.of("role", "PHARMACIST");

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request, headers), Void.class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String details = StringUtils.hasText(body) ? body : ex.getStatusText();
            throw new IllegalStateException(
                "Failed to assign PHARMACIST role in user-service ("
                    + ex.getRawStatusCode() + "): " + details
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
                "Failed to assign PHARMACIST role in user-service: " + ex.getMessage()
            );
        }
    }
}
