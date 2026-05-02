package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSeverityPredictRequest {
    @NotBlank(message = "Diagnosis text is required")
    private String diagnosis;
}
