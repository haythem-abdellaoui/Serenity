package com.example.pharmacy.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerMedicineAlternativeDTO {

    private Long lineId;
    private String medicationName;
    private Integer requiredQuantity;
    private List<AlternativePharmacyOptionDTO> pharmacies;
}
