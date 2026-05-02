package tn.esprit.arctic.derbelmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.arctic.derbelmicroservice.dto.request.AiDrugRecommendRequest;
import tn.esprit.arctic.derbelmicroservice.dto.response.AiDrugRecommendResponse;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;
import tn.esprit.arctic.derbelmicroservice.service.IAiRecommendationService;

@RestController
@RequestMapping("/records/ai-prescriptions")
@RequiredArgsConstructor
public class AiPrescriptionController {

    private final IAiRecommendationService aiRecommendationService;

    @PostMapping("/recommend-drugs")
    public ResponseEntity<ApiResponseDTO<AiDrugRecommendResponse>> recommendDrugs(
            @Valid @RequestBody AiDrugRecommendRequest request) {

        DerbelAuth.requireDoctorOrAdmin(); // Protect endpoint

        AiDrugRecommendResponse recommendation = aiRecommendationService.recommendDrugs(request);

        return ResponseEntity.ok(ApiResponseDTO.<AiDrugRecommendResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Médicaments recommandés par l'IA")
                .data(recommendation)
                .build());
    }
}
