package com.serenity.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodEntryResponseDTO {

    private Long id;
    private Long patientId;
    private String patientName;
    /** Profile avatar URL from {@code user_profiles.avatar} (optional). */
    private String patientAvatarUrl;
    private Long doctorId;  // Assigned doctor who tracks this patient's mood entries
    private String doctorName;
    private Integer moodScore;
    private String moodDescription;
    private String triggers;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date updatedAt;

    /** HIGH_RISK, MEDIUM_RISK, LOW_RISK from monitoring-ai (optional). */
    private String aiRiskLevel;
    private Double aiRiskConfidence;
    private String aiRiskRecommendation;
    /** Generic risk subtype (HIGH/MEDIUM), e.g. SUICIDAL_CRISIS or ANXIETY_DISTRESS. */
    private String aiRiskType;
    /** Present when aiRiskLevel == MEDIUM_RISK (for example ANXIETY_DISTRESS). */
    private String aiMediumRiskType;
    private Integer aiRiskScore;
}
