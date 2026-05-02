package com.example.insurance.integration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClaimRiskModelClient {

    private static final Logger log = LoggerFactory.getLogger(ClaimRiskModelClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.claim-risk-model.url:http://localhost:5123}")
    private String claimRiskModelUrl;

    public ClaimRiskModelScoreResponse score(Map<String, Object> requestBody) {
        try {
            ResponseEntity<ClaimRiskModelScoreResponse> resp = restTemplate.postForEntity(
                    claimRiskModelUrl + "/score",
                    requestBody,
                    ClaimRiskModelScoreResponse.class
            );
            return resp.getBody();
        } catch (RestClientException e) {
            log.warn("Claim risk model call failed (url={}): {}", claimRiskModelUrl, e.getMessage());
            return null;
        }
    }
}

