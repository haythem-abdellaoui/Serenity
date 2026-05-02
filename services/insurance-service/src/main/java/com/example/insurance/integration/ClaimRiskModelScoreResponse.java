package com.example.insurance.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for the Python risk-model response (snake_case).
 */
@Getter
@Setter
@NoArgsConstructor
public class ClaimRiskModelScoreResponse {
    @JsonProperty("risk_score")
    private Double riskScore;

    @JsonProperty("risk_band")
    private String riskBand;

    @JsonProperty("top_reasons")
    private List<String> topReasons;
}

