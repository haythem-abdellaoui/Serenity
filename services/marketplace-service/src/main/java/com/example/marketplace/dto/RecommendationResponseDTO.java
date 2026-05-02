package com.example.marketplace.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDTO {
    private List<ProductRecommendationItemDTO> recommendations;
    private String reasoning;
    private Integer totalRecommendations;
    private LocalDateTime generatedAt;
}
