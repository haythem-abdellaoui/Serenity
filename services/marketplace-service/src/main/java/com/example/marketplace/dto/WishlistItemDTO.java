package com.example.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.marketplace.entity.ProductType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private ProductType productType;
    private Boolean productPreviewable;
    private LocalDateTime addedAt;
}
