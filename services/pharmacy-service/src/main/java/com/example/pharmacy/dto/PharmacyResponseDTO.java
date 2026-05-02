package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyResponseDTO {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String licenseNumber;
    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;
    private Boolean supportsEmergency;
    private String createdAt;
    private String updatedAt;
}
