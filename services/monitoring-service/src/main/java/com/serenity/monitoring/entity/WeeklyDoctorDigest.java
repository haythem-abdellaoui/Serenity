package com.serenity.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(
        name = "weekly_doctor_digests",
        uniqueConstraints = @UniqueConstraint(name = "uk_digest_doctor_week", columnNames = {"doctor_id", "week_start_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  WeeklyDoctorDigest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "crisis_count", nullable = false)
    private Integer crisisCount;

    @Column(name = "worsening_patients", nullable = false)
    private Integer worseningPatients;

    @Column(name = "no_checkin_patients", nullable = false)
    private Integer noCheckinPatients;

    @Column(name = "summary_message", nullable = false, length = 1000)
    private String summaryMessage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Date generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = new Date();
        }
    }
} 

