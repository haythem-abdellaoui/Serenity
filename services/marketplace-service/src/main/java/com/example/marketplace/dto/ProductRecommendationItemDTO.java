package com.example.marketplace.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendationItemDTO {
    private Long productId;
    private String productName;
    private String category;
    private String reason;
    private Integer confidence;
}
