package tn.esprit.arctic.derbelmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.arctic.derbelmicroservice.dto.request.AiSeverityPredictRequest;
import tn.esprit.arctic.derbelmicroservice.dto.response.AiSeverityPredictResponse;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;
import tn.esprit.arctic.derbelmicroservice.service.IAiSeverityService;

@RestController
@RequestMapping("/records/ai-severity")
@RequiredArgsConstructor
public class AiSeverityController {

    private final IAiSeverityService aiSeverityService;

    @PostMapping("/predict")
    public ResponseEntity<ApiResponseDTO<AiSeverityPredictResponse>> predictSeverity(
            @Valid @RequestBody AiSeverityPredictRequest request) {

        DerbelAuth.requireDoctorOrAdmin(); // Protect endpoint

        AiSeverityPredictResponse prediction = aiSeverityService.predictSeverity(request);

        return ResponseEntity.ok(ApiResponseDTO.<AiSeverityPredictResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Sévérité prédite par l'IA")
                .data(prediction)
                .build());
    }
}
