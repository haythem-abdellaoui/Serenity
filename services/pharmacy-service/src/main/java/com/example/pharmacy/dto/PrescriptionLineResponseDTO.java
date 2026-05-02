package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionLineResponseDTO {

    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private Integer quantity;
    private String startDate;
    private String endDate;
    private String instructions;
}
