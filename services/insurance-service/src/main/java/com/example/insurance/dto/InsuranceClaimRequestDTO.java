package com.example.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaimRequestDTO {

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000.00", message = "Amount cannot exceed 100000")
    @Digits(integer = 6, fraction = 2, message = "Amount format is invalid")
    private Double amount;

    @NotBlank(message = "Insurance company is required")
    @Size(max = 100, message = "Insurance company is too long")
    private String insuranceCompany;

    @NotNull(message = "Insurance grade is required")
    @DecimalMin(value = "1.0", message = "Insurance grade must be between 1 and 5")
    @DecimalMax(value = "5.0", message = "Insurance grade must be between 1 and 5")
    private Double insuranceGrade;
}
