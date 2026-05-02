package com.example.marketplace.clients;

import com.example.marketplace.dto.PrescriptionDTO;
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
public class PharmacyServiceClient {

    private final RestTemplate restTemplate;
    
    private static final String PHARMACY_SERVICE_URL = "http://localhost:8082";
    private static final String GET_PRESCRIPTIONS_ENDPOINT = "/api/pharmacy/prescriptions/mine";

    public List<PrescriptionDTO> getUserAcceptedPrescriptions(Long userId, String jwtToken) {
        try {
            String url = PHARMACY_SERVICE_URL + GET_PRESCRIPTIONS_ENDPOINT;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<PrescriptionDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<PrescriptionDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} accepted prescriptions for user {}", 
                    response.getBody().size(), userId);
                return response.getBody();
            }

            log.warn("Unexpected response code from pharmacy service: {}", response.getStatusCode());
            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Error calling pharmacy service for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Unexpected error fetching prescriptions for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
