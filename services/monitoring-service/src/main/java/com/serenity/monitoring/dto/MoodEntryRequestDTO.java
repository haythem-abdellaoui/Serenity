package com.serenity.monitoring.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodEntryRequestDTO {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    // doctorId is auto-assigned by the service, not required in request
    private Long doctorId;  // Will be populated by service

    @NotNull(message = "Mood score is required")
    @Min(value = 1, message = "Mood score must be between 1 and 10")
    @Max(value = 10, message = "Mood score must be between 1 and 10")
    private Integer moodScore;

    @NotBlank(message = "Mood description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String moodDescription;

    @Size(max = 500, message = "Triggers must not exceed 500 characters")
    private String triggers;  // Optional triggers
}
