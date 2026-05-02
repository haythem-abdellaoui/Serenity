package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDrugRecommendResponse {
    private List<String> recommended_drugs;
    private String error;
}
