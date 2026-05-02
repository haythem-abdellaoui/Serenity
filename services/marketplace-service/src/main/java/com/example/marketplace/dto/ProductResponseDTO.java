package com.example.marketplace.dto;

import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.PreviewContentType;
import com.example.marketplace.entity.ProductType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private ProductCategory category;
    private ProductType type;
    private BigDecimal price;
    private Boolean active;
    private String imageUrl;
    private Boolean previewable;
    private PreviewContentType previewType;
    private String previewUrl;
    private String contentUrl;
}
