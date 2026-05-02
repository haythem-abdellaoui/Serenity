package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemRequestDTO {

    @NotNull(message = "L'ID du médicament est obligatoire")
    private Long medicineId;

    @NotBlank(message = "Le dosage est obligatoire")
    @Size(max = 50)
    private String dosage;

    @NotBlank(message = "La fréquence est obligatoire")
    @Size(max = 50)
    private String frequency;

    @Min(value = 1, message = "La quantité doit être au minimum 1")
    private int quantity;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500)
    private String instructions;

    // Tracks if this medication was suggested by the AI recommender
    private Boolean isAiRecommended;
}
