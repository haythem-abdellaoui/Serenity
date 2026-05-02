package com.example.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "pharmacy_prescriptions",
    uniqueConstraints = @UniqueConstraint(name = "uk_pharmacy_prescriptions_source_id", columnNames = "source_prescription_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyPrescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_prescription_id", nullable = false, unique = true)
    private Long sourcePrescriptionId;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_pharmacy_id")
    private Pharmacy assignedPharmacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrescriptionStatus status;

    @Column(length = 1500)
    private String rejectionReason;

    private LocalDateTime readyAt;

    @Column(name = "insurance_document_path")
    private String insuranceDocumentPath;

    @Column(name = "insurance_document_uploaded_at")
    private LocalDateTime insuranceDocumentUploadedAt;

    @Column(nullable = false)
    private String patientNameSnapshot;

    @Column(nullable = false)
    private String doctorNameSnapshot;

    @Column(nullable = false)
    private LocalDateTime sourceCreatedAt;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
        if (this.status == null) {
            this.status = PrescriptionStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
