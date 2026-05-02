package com.example.insurance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InsuranceClaimOcrAuditResponseDTO {
    private Long id;
    private Long claimId;
    private Long userId;
    private String attemptType;
    private String decision;
    private Double submittedAmount;
    private String submittedCompany;
    private Double extractedAmount;
    private String extractedInvoiceDate;
    private String extractedProviderName;
    private Integer mismatchCount;
    private Integer majorCount;
    private Integer minorCount;
    private String mismatchSummary;
    private String mismatchDetailsJson;
    private LocalDateTime createdAt;
}
