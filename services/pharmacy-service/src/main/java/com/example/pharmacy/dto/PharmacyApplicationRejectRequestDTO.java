package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyApplicationRejectRequestDTO {

    @NotBlank(message = "Review comment is required when rejecting an application")
    private String reviewComment;
}
