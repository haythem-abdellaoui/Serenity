package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.esprit.arctic.derbelmicroservice.entity.enums.Severity;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordRequestDTO {

    @NotBlank(message = "Le diagnostic est obligatoire")
    @Size(max = 255, message = "Le diagnostic ne doit pas dépasser 255 caractères")
    private String diagnosis;

    @Size(max = 2000, message = "Les notes ne doivent pas dépasser 2000 caractères")
    private String notes;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "La sévérité est obligatoire (LOW, MEDIUM, HIGH)")
    private Severity severity;

    @NotBlank(message = "Le statut est obligatoire (ACTIVE, CLOSED)")
    private String status;

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    private Long doctorId;
}
