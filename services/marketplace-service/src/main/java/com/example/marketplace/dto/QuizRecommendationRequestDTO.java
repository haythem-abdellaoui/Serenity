package com.example.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizRecommendationRequestDTO {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer anxietyLevel;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer stressLevel;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer sleepNeed;
}
