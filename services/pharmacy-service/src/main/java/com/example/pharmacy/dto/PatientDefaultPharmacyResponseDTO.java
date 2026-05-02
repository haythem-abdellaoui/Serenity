package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDefaultPharmacyResponseDTO {

    private Long patientId;
    private Long pharmacyId;
    private String pharmacyName;
    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;
    private Boolean supportsEmergency;
    private String selectedAt;
}
