package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternativePharmacyOptionDTO {

    private Long pharmacyId;
    private String pharmacyName;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Integer availableQuantity;
}
