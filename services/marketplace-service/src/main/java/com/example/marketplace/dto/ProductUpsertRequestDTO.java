package com.example.marketplace.dto;

import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.PreviewContentType;
import com.example.marketplace.entity.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpsertRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotNull
    private ProductCategory category;

    @NotNull
    private ProductType type;

    @NotNull
    @DecimalMin(value = "0.10")
    private BigDecimal price;

    @Size(max = 500)
    private String imageUrl;

    @NotNull
    private Boolean previewable;

    private PreviewContentType previewType;

    @Size(max = 1000)
    private String previewUrl;

    @Size(max = 1000)
    private String contentUrl;

    @NotNull
    private Boolean active;
}
