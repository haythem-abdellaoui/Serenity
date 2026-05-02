package com.example.marketplace.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDTO {
    private Long id;
    private String medicationName;
    private String dosage;
    private String status;
    private LocalDateTime createdAt;
    private List<MedicationLineDTO> medicationLines;
}
