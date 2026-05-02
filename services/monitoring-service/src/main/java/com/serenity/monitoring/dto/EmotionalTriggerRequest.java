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
public class EmotionalTriggerRequest {

    @NotNull(message = "moodEntryId is required")
    private Long moodEntryId;

    @NotBlank(message = "triggerType is required")
    @Size(max = 100, message = "triggerType must not exceed 100 characters")
    @Pattern(
            regexp = "^(WORK_STRESS|SLEEP_DEPRIVATION|FAMILY_CONFLICT|SOCIAL_ISOLATION|TRAUMA|FINANCIAL_STRESS|COGNITIVE_OVERLOAD|OTHER)$",
            message = "triggerType is invalid"
    )
    private String triggerType;

    @NotBlank(message = "description is required")
    @Size(min = 10, max = 1000, message = "description must be between 10 and 1000 characters")
    private String description;

    @NotNull(message = "intensity is required")
    @Min(value = 1, message = "intensity must be between 1 and 10")
    @Max(value = 10, message = "intensity must be between 1 and 10")
    private Integer intensity;
}

