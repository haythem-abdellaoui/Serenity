package com.serenity.monitoring.integration;

import com.serenity.monitoring.config.MonitoringAiProperties;
import com.serenity.monitoring.dto.MonitoringAiCrisisRequest;
import com.serenity.monitoring.dto.MonitoringAiCrisisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringAiCrisisClient {

    private final RestClient monitoringAiRestClient;
    private final MonitoringAiProperties properties;

    public Optional<MonitoringAiCrisisResponse> predict(MonitoringAiCrisisRequest request) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            MonitoringAiCrisisResponse body = monitoringAiRestClient.post()
                    .uri("/predict/crisis")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MonitoringAiCrisisResponse.class);
            return Optional.ofNullable(body);
        } catch (RestClientException ex) {
            log.warn("Monitoring AI unavailable at {}: {}", properties.getUrl(), ex.getMessage());
            return Optional.empty();
        }
    }
}
