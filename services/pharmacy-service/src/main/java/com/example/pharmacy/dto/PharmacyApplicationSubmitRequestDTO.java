package com.example.pharmacy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyApplicationSubmitRequestDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "CIN number is required")
    @Pattern(regexp = "^\\d{8}$", message = "CIN number must be exactly 8 digits")
    private String cinNumber;

    @NotBlank(message = "CNOP/CNOPT number is required")
    private String cnopNumber;

    @NotBlank(message = "Pharmacy name is required")
    private String pharmacyName;

    @NotBlank(message = "Authorization reference number is required")
    private String authorizationReferenceNumber;

    @NotBlank(message = "Address is required")
    private String addressLine;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Governorate is required")
    private String governorate;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @Pattern(regexp = "^$|^\\d{8}$", message = "Phone must be exactly 8 digits")
    private String phone;
    private String openingHours;
}
