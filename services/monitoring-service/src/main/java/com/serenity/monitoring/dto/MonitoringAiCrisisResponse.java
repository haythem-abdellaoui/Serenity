package com.serenity.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from POST /predict/crisis on the monitoring-ai FastAPI service.
 */
public record MonitoringAiCrisisResponse(
        @JsonProperty("patient_id") long patientId,
        @JsonProperty("risk_level") String riskLevel,
        double confidence,
        String message,
        String recommendation,
        @JsonProperty("risk_score") int riskScore,
        @JsonProperty("risk_type") String riskType,
        @JsonProperty("medium_risk_type") String mediumRiskType
) {
}
