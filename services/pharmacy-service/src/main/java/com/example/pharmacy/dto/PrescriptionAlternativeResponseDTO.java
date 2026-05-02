package com.example.pharmacy.dto;

import com.example.pharmacy.entity.PrescriptionStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionAlternativeResponseDTO {

    private Long prescriptionId;
    private PrescriptionStatus status;
    private Double latitude;
    private Double longitude;
    private Double fullMatchRadiusKm;
    private Double partialRadiusKm;
    private String recommendedMode;
    private String message;
    private List<AlternativePharmacyOptionDTO> fullMatchPharmacies;
    private List<PerMedicineAlternativeDTO> perMedicineAlternatives;
    private List<AlternativePharmacyOptionDTO> selectablePharmacies;
}
