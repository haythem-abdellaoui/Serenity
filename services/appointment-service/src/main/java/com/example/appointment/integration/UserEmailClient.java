package com.example.appointment.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fetches user emails from user-service (internal API) for reminder mail.
 */
@Slf4j
@Component
public class UserEmailClient {

    private static final AtomicBoolean LOGGED_MISSING_INTERNAL_KEY = new AtomicBoolean(false);

    private final RestClient userServiceRestClient;

    @Value("${app.internal-api-key:}")
    private String internalApiKey;

    public UserEmailClient(@Qualifier("userServiceInternalRestClient") RestClient userServiceRestClient) {
        this.userServiceRestClient = userServiceRestClient;
    }

    public Map<Long, String> fetchEmailsByUserIds(List<Long> ids) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            if (LOGGED_MISSING_INTERNAL_KEY.compareAndSet(false, true)) {
                log.warn("app.internal-api-key / INTERNAL_API_KEY is empty — cannot load user emails for notifications. "
                        + "Set the same value as user-service (INTERNAL_API_KEY environment variable).");
            }
            return Map.of();
        }
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserEmailResponse> list = userServiceRestClient.post()
                    .uri("/api/internal/users/emails-by-ids")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Internal-Key", internalApiKey)
                    .body(new LookupIdsRequest(ids))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserEmailResponse>>() {});
            if (list == null || list.isEmpty()) {
                log.warn("Internal email lookup returned no rows for userIds={} (users missing or no email in DB?)", ids);
                return Map.of();
            }
            Map<Long, String> map = new HashMap<>();
            for (UserEmailResponse r : list) {
                if (r.getId() != null && r.getEmail() != null && !r.getEmail().isBlank()) {
                    map.put(r.getId(), r.getEmail());
                }
            }
            if (map.isEmpty() && !ids.isEmpty()) {
                log.warn("Internal email lookup returned {} row(s) but none had usable emails for ids={}", list.size(), ids);
            } else {
                log.debug("Resolved {} email(s) for notification delivery", map.size());
            }
            return map;
        } catch (RestClientResponseException e) {
            log.warn("Internal email lookup failed: status={} body={}", e.getStatusCode().value(),
                    e.getResponseBodyAsString(StandardCharsets.UTF_8));
            return Map.of();
        } catch (Exception e) {
            log.warn("Could not fetch user emails: {}", e.getMessage());
            return Map.of();
        }
    }
}
