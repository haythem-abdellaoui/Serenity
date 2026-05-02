package com.example.insurance.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaimResponseDTO {

    private Long id;
    private String description;
    private Date claimDate;
    private Double amount;
    private Double reimbursementAmount;
    private String insuranceCompany;
    private Double insuranceGrade;
    private String reason;
    private String status;
    private String externalRef;
    private List<String> filePaths;
    private Long userId;
    private String infoRequestReason;
    private Date infoRequestDeadline;
    private Date infoRequestedAt;
    private Date infoRespondedAt;
    private String ocrLastDecision;
    private Integer ocrMismatchCount;
    private Date ocrLastAnalyzedAt;
    private String ocrSummary;
    private List<RemboursementResponseDTO> remboursements;
}
