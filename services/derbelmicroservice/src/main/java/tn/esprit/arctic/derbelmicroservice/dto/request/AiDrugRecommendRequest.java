package tn.esprit.arctic.derbelmicroservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDrugRecommendRequest {
    private String diagnosis;
}
