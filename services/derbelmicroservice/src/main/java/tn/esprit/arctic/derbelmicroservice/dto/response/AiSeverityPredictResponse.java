package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSeverityPredictResponse {
    private tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity;
    private Double confidence;
    private Map<String, Double> probabilities;
}
