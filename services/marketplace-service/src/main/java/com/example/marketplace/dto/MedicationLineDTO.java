package com.example.marketplace.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationLineDTO {
    private String medicationName;
    private String strength;
    private String quantity;
}
