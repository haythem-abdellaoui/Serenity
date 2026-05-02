package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyUpsertRequestDTO {

    @NotBlank(message = "Pharmacy name is required")
    private String name;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @Pattern(regexp = "^$|^\\d{8}$", message = "Phone must be exactly 8 digits")
    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;

    @NotNull(message = "Pharmacy latitude is required")
    private Double latitude;

    @NotNull(message = "Pharmacy longitude is required")
    private Double longitude;

    private Boolean supportsEmergency;
}
