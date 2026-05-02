package com.example.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "teleconsultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teleconsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false, length = 1024)
    private String meetingUrl;

    @Column(nullable = false)
    private Integer durationMinutes;

    private Instant startTime;

    private Instant endTime;

    @Column(length = 1024)
    private String recordingUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TeleconsultationStatus status;
}
