package com.example.pharmacy.dto;

import com.example.pharmacy.entity.StockState;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItemResponseDTO {

    private Long id;
    private Long pharmacyId;
    private String medicineName;
    private Integer quantity;
    private String imageUrl;
    private String description;
    private StockState state;
    private Boolean archived;
    private String createdAt;
    private String updatedAt;
}
