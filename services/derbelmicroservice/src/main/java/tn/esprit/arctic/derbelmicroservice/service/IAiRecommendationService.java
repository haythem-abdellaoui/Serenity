package tn.esprit.arctic.derbelmicroservice.service;

import tn.esprit.arctic.derbelmicroservice.dto.request.AiDrugRecommendRequest;
import tn.esprit.arctic.derbelmicroservice.dto.response.AiDrugRecommendResponse;

public interface IAiRecommendationService {
    AiDrugRecommendResponse recommendDrugs(AiDrugRecommendRequest request);
}
