package com.serenity.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotional_trigger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmotionalTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mood_entry_id", nullable = false)
    private MoodEntry moodEntry;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "trigger_type", length = 100, nullable = false)
    private String triggerType;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "intensity", nullable = false)
    private Integer intensity;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}

