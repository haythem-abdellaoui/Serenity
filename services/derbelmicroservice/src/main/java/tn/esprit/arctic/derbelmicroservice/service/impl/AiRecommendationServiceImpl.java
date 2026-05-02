package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.arctic.derbelmicroservice.dto.request.AiDrugRecommendRequest;
import tn.esprit.arctic.derbelmicroservice.dto.response.AiDrugRecommendResponse;
import tn.esprit.arctic.derbelmicroservice.service.IAiRecommendationService;

@Service
@Slf4j
public class AiRecommendationServiceImpl implements IAiRecommendationService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5001}")
    private String aiServiceUrl;

    public AiRecommendationServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AiDrugRecommendResponse recommendDrugs(AiDrugRecommendRequest request) {
        if (request == null || request.getDiagnosis() == null || request.getDiagnosis().isEmpty()) {
            throw new IllegalArgumentException("Diagnosis cannot be null or empty");
        }

        String endpoint = aiServiceUrl + "/recommend-drugs";
        
        try {
            log.info("Calling AI Drug Recommendation service for diagnosis: {}", request.getDiagnosis());
            ResponseEntity<AiDrugRecommendResponse> response = restTemplate.postForEntity(
                    endpoint,
                    request,
                    AiDrugRecommendResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("AI service returned non-success status: {}", response.getStatusCode());
                return AiDrugRecommendResponse.builder().error("Failed to get recommendation from AI service").build();
            }
        } catch (Exception e) {
            log.error("Error communicating with AI service: {}", e.getMessage());
            return AiDrugRecommendResponse.builder().error("Error communicating with AI service: " + e.getMessage()).build();
        }
    }
}
