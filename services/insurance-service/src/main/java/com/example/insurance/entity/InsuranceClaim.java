package com.example.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "insurance_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date claimDate;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "reimbursement_amount", nullable = false)
    private Double reimbursementAmount;

    @Column(name = "insurance_company", nullable = false)
    private String insuranceCompany;

    @Column(name = "insurance_grade", nullable = false)
    private Double insuranceGrade;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(unique = true)
    private String externalRef;

    @Column(nullable = false)
    private Long userId;

    @ElementCollection
    @CollectionTable(name = "claim_files", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "file_path")
    @Builder.Default
    private List<String> filePaths = new ArrayList<>();

    @OneToMany(mappedBy = "insuranceClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Remboursement> remboursements = new ArrayList<>();

    @Column(name = "info_request_reason", length = 1000)
    private String infoRequestReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "info_request_deadline")
    private Date infoRequestDeadline;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "info_requested_at")
    private Date infoRequestedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "info_responded_at")
    private Date infoRespondedAt;

    @OneToMany(mappedBy = "insuranceClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<InsuranceClaimTransition> transitions = new ArrayList<>();

    @Column(name = "ocr_last_decision", length = 40)
    private String ocrLastDecision;

    @Column(name = "ocr_mismatch_count")
    private Integer ocrMismatchCount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ocr_last_analyzed_at")
    private Date ocrLastAnalyzedAt;

    @Column(name = "ocr_summary", length = 1500)
    private String ocrSummary;

    @PrePersist
    protected void onCreate() {
        this.claimDate = new Date();
    }
}
