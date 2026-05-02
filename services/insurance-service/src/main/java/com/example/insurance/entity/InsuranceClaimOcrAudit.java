package com.example.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_claim_ocr_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaimOcrAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 40)
    private OcrAttemptType attemptType;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 40)
    private OcrAnalysisDecision decision;

    @Column(name = "submitted_amount")
    private Double submittedAmount;

    @Column(name = "submitted_company", length = 120)
    private String submittedCompany;

    @Column(name = "extracted_amount")
    private Double extractedAmount;

    @Column(name = "extracted_invoice_date", length = 40)
    private String extractedInvoiceDate;

    @Column(name = "extracted_provider_name", length = 180)
    private String extractedProviderName;

    @Column(name = "mismatch_count")
    private Integer mismatchCount;

    @Column(name = "major_count")
    private Integer majorCount;

    @Column(name = "minor_count")
    private Integer minorCount;

    @Column(name = "mismatch_summary", length = 2000)
    private String mismatchSummary;

    @Lob
    @Column(name = "mismatch_details_json")
    private String mismatchDetailsJson;

    @Lob
    @Column(name = "extracted_text")
    private String extractedText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
