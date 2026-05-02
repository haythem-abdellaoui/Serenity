package com.example.marketplace.clients;

import com.example.marketplace.dto.InsuranceClaimDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceServiceClient {

    private final RestTemplate restTemplate;
    
    private static final String INSURANCE_SERVICE_URL = "http://localhost:8082";
    private static final String GET_CLAIMS_ENDPOINT = "/api/insurance/claims/me";

    public List<InsuranceClaimDTO> getUserApprovedClaims(Long userId, String jwtToken) {
        try {
            String url = INSURANCE_SERVICE_URL + GET_CLAIMS_ENDPOINT;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<List<InsuranceClaimDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<InsuranceClaimDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} approved claims for user {}", 
                    response.getBody().size(), userId);
                return response.getBody();
            }

            log.warn("Unexpected response code from insurance service: {}", response.getStatusCode());
            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Error calling insurance service for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Unexpected error fetching insurance claims for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
