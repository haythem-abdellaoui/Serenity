package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionRequestDTO {

    @NotNull(message = "L'ID du dossier médical est obligatoire")
    private Long medicalRecordId;

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    private Long doctorId;

    private String status;

    @NotEmpty(message = "Au moins un item est obligatoire")
    private List<@Valid PrescriptionItemRequestDTO> items;

    // Optional field for Cloudinary image upload from the frontend
    private String imageBase64;
}
