package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyCandidateResponseDTO {

    private Long id;
    private String name;
    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;
    private Boolean supportsEmergency;
    private Double distanceKm;
}
