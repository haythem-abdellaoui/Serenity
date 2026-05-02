package com.serenity.monitoring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "mood_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;  // Assigned doctor for this patient (Round-Robin assignment)

    @Column(nullable = false)
    private Integer moodScore;  // 1-10 scale

    @Column(columnDefinition = "TEXT")
    private String moodDescription;  // Renamed from 'condition' (reserved keyword)

    @Column(columnDefinition = "TEXT")
    private String triggers;  // Emotional triggers (comma-separated or JSON)

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    /** From monitoring-ai: HIGH_RISK, MEDIUM_RISK, LOW_RISK */
    @Column(name = "ai_risk_level", length = 32)
    private String aiRiskLevel;

    @Column(name = "ai_risk_confidence")
    private Double aiRiskConfidence;

    @Column(name = "ai_risk_recommendation", columnDefinition = "TEXT")
    private String aiRiskRecommendation;

    /** Dataset-derived type for HIGH/MEDIUM risk (for example SUICIDAL_CRISIS, ANXIETY_DISTRESS). */
    @Column(name = "ai_risk_type", length = 64)
    private String aiRiskType;

    /** Populated only when aiRiskLevel = MEDIUM_RISK. */
    @Column(name = "ai_medium_risk_type", length = 64)
    private String aiMediumRiskType;

    /** 1=low, 2=medium, 3=high (mirrors Python risk_score) */
    @Column(name = "ai_risk_score")
    private Integer aiRiskScore;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }
}
