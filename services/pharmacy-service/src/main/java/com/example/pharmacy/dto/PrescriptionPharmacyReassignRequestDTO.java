package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionPharmacyReassignRequestDTO {

    @NotNull(message = "Pharmacy id is required")
    @Positive(message = "Pharmacy id must be positive")
    private Long pharmacyId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
