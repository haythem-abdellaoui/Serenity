package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemResponseDTO {

    private Long id;
    private MedicineResponseDTO medicine;
    private String dosage;
    private String frequency;
    private int quantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String instructions;
    private Boolean isAiRecommended;
}
