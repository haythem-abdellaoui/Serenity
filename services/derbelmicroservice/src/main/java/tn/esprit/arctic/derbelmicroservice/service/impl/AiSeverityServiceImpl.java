package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.arctic.derbelmicroservice.dto.request.AiSeverityPredictRequest;
import tn.esprit.arctic.derbelmicroservice.dto.response.AiSeverityPredictResponse;
import tn.esprit.arctic.derbelmicroservice.service.IAiSeverityService;

@Service
@Slf4j
public class AiSeverityServiceImpl implements IAiSeverityService {

    private final RestTemplate restTemplate;

    @Value("${ai.severity.api.url:http://localhost:5001/predict}")
    private String aiApiUrl;

    // Use default RestTemplate if a bean doesn't exist to avoid missing bean exceptions
    public AiSeverityServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AiSeverityPredictResponse predictSeverity(AiSeverityPredictRequest request) {
        log.info("🤖 Requesting AI severity prediction for: {}", request.getDiagnosis());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiSeverityPredictRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AiSeverityPredictResponse> response = restTemplate.postForEntity(
                    aiApiUrl,
                    entity,
                    AiSeverityPredictResponse.class
            );

            log.info("🤖 AI Prediction result: {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("❌ Failed to get prediction from AI service", e);
            // Default fallback if AI service is down
            return AiSeverityPredictResponse.builder()
                    .severity(tn.esprit.arctic.derbelmicroservice.entity.enums.Severity.MEDIUM)
                    .confidence(0.0)
                    .build();
        }
    }
}
