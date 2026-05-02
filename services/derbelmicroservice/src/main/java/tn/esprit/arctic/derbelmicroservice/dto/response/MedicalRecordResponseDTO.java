package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;
import tn.esprit.arctic.derbelmicroservice.entity.enums.Severity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordResponseDTO {

    private Long id;
    private String diagnosis;
    private String notes;
    private LocalDate date;
    private Severity severity;
    private String status;
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private Long doctorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
