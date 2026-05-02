package com.example.insurance.dto;

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
public class SubmitAdditionalDocumentsDTO {

    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000.00", message = "Amount cannot exceed 100000")
    @Digits(integer = 6, fraction = 2, message = "Amount format is invalid")
    private Double amount;

    @DecimalMin(value = "1.0", message = "Insurance grade must be between 1 and 5")
    @DecimalMax(value = "5.0", message = "Insurance grade must be between 1 and 5")
    private Double insuranceGrade;
}
