package com.example.insurance.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class InsurancePortalClient {

    private static final Logger log = LoggerFactory.getLogger(InsurancePortalClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.insurance-portal.api-url}")
    private String portalApiUrl;

    public void submitClaim(PortalSubmitClaimRequest request) {
        try {
            ResponseEntity<Void> resp = restTemplate.postForEntity(
                    portalApiUrl + "/claims",
                    request,
                    Void.class
            );
            log.info("Submitted claim to portal, externalRef={}, status={}", request.getRef(), resp.getStatusCode());
        } catch (RestClientException e) {
            log.warn("Failed to submit claim to portal, externalRef={}, error={}", request.getRef(), e.getMessage());
        }
    }

    public PortalClaimStatusResponse fetchClaimStatus(String ref) {
        try {
            return restTemplate.getForObject(
                    portalApiUrl + "/claims/{ref}/status",
                    PortalClaimStatusResponse.class,
                    ref
            );
        } catch (RestClientException e) {
            log.warn("Failed to fetch portal claim status, externalRef={}, error={}", ref, e.getMessage());
            return null;
        }
    }

    public void notifyResubmission(String ref, PortalResubmissionRequest request) {
        try {
            restTemplate.postForEntity(
                    portalApiUrl + "/claims/{ref}/resubmission",
                    request,
                    Void.class,
                    ref
            );
            log.info("Notified portal claim resubmission, externalRef={}", ref);
        } catch (RestClientException e) {
            log.warn("Failed to notify portal claim resubmission, externalRef={}, error={}", ref, e.getMessage());
        }
    }
}

