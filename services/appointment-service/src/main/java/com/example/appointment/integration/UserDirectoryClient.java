package com.example.appointment.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserDirectoryClient {

    private final RestClient userServiceRestClient;

    public UserDirectoryClient(@Qualifier("userServiceRestClient") RestClient userServiceRestClient) {
        this.userServiceRestClient = userServiceRestClient;
    }

    public Map<Long, UserLookupSnippet> resolveNamesById(Collection<Long> userIds, String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserLookupSnippet> list = userServiceRestClient.post()
                    .uri("/api/users/lookup/names")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new LookupIdsRequest(distinct))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserLookupSnippet>>() {});
            if (list == null || list.isEmpty()) {
                return Map.of();
            }
            return list.stream().collect(Collectors.toMap(UserLookupSnippet::getId, u -> u, (a, b) -> a));
        } catch (RestClientResponseException e) {
            log.warn("User name lookup failed: status={} body={}", e.getStatusCode().value(),
                    e.getResponseBodyAsString(StandardCharsets.UTF_8));
            return Map.of();
        } catch (Exception e) {
            log.warn("Could not resolve user names from user-service: {}", e.getMessage());
            return Map.of();
        }
    }
}
