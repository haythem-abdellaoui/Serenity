package tn.esprit.arctic.derbelmicroservice.service;

import tn.esprit.arctic.derbelmicroservice.dto.request.AiSeverityPredictRequest;
import tn.esprit.arctic.derbelmicroservice.dto.response.AiSeverityPredictResponse;

public interface IAiSeverityService {
    AiSeverityPredictResponse predictSeverity(AiSeverityPredictRequest request);
}
