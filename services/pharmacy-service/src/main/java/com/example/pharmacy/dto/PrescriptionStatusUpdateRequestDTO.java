package com.example.pharmacy.dto;

import com.example.pharmacy.entity.PrescriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionStatusUpdateRequestDTO {

    @NotNull(message = "Status is required")
    private PrescriptionStatus status;

    private String rejectionReason;
}
