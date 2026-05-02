package com.example.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacy_onboarding_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyOnboardingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PharmacyApplicationStatus status;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedByAdminId;

    @Column(length = 1500)
    private String reviewComment;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String cinNumber;

    @Column(nullable = false)
    private String cnopNumber;

    @Column(nullable = false)
    private String pharmacyName;

    @Column(nullable = false)
    private String authorizationReferenceNumber;

    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private String cinDocumentPath;

    @Column(nullable = false)
    private String cnoptProofPath;

    @Column(nullable = false)
    private String pharmacyAuthorizationPath;

    @Column(length = 32)
    private String cnoptMlDecision;

    @Column(length = 32)
    private String cnoptMlFraudStatus;

    @Column(length = 1500)
    private String cnoptMlMessage;

    private Double cnoptMlRiskScore;

    private LocalDateTime cnoptMlCheckedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
