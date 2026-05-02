package com.example.insurance.service.impl;

import com.example.insurance.dto.ClaimRiskScoreResponseDTO;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.InsuranceClaimOcrAudit;
import com.example.insurance.integration.ClaimRiskModelClient;
import com.example.insurance.integration.ClaimRiskModelScoreResponse;
import com.example.insurance.repository.InsuranceClaimOcrAuditRepository;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.service.ClaimRiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimRiskScoringServiceImpl implements ClaimRiskScoringService {

    private final InsuranceClaimRepository claimRepository;
    private final InsuranceClaimOcrAuditRepository ocrAuditRepository;
    private final ClaimRiskModelClient claimRiskModelClient;

    @Override
    public ClaimRiskScoreResponseDTO scoreClaim(Long claimId) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        Long userId = claim.getUserId();

        Map<String, Object> payload = new HashMap<>();

        // Required fields (InsuranceClaim)
        payload.put("claim_id", claim.getId());
        payload.put("user_id", userId);
        payload.put("amount", claim.getAmount());
        payload.put("reimbursement_amount", claim.getReimbursementAmount());
        payload.put("insurance_company", claim.getInsuranceCompany());
        payload.put("insurance_grade", claim.getInsuranceGrade());
        payload.put("file_count", claim.getFilePaths() == null ? 0 : claim.getFilePaths().size());

        List<InsuranceClaimOcrAudit> audits = ocrAuditRepository.findByClaimIdOrderByCreatedAtDesc(claim.getId());
        InsuranceClaimOcrAudit latest = audits.isEmpty() ? null : audits.get(0);
        if (latest != null) {
            payload.put("ocr_decision", latest.getDecision() == null ? null : latest.getDecision().name());
            payload.put("ocr_mismatch_count", latest.getMismatchCount() == null ? 0 : latest.getMismatchCount());
            payload.put("ocr_major_count", latest.getMajorCount() == null ? 0 : latest.getMajorCount());
            payload.put("ocr_minor_count", latest.getMinorCount() == null ? 0 : latest.getMinorCount());
            payload.put("ocr_submitted_amount", latest.getSubmittedAmount());
            payload.put("ocr_extracted_amount", latest.getExtractedAmount());
        }

        payload.put("user_total_claims", (int) claimRepository.countByUserId(userId));
        payload.put("user_rejected_claims", (int) claimRepository.countByUserIdAndStatus(userId, ClaimStatus.REJECTED));

        Date since30d = Date.from(Instant.now().minus(Duration.ofDays(30)));
        payload.put("user_claims_30d", (int) claimRepository.countByUserIdAndClaimDateAfter(userId, since30d));

        Double daysSinceLast = resolveDaysSinceLastClaim(claim);
        payload.put("days_since_last_claim", daysSinceLast);

        ClaimRiskModelScoreResponse modelResp = claimRiskModelClient.score(payload);
        if (modelResp == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Risk model unavailable");
        }
        return ClaimRiskScoreResponseDTO.builder()
                .riskScore(modelResp.getRiskScore())
                .riskBand(modelResp.getRiskBand())
                .topReasons(modelResp.getTopReasons())
                .build();
    }

    private Double resolveDaysSinceLastClaim(InsuranceClaim currentClaim) {
        List<InsuranceClaim> claims = claimRepository.findByUserIdOrderByClaimDateDesc(currentClaim.getUserId());
        Date now = new Date();
        for (InsuranceClaim c : claims) {
            if (c.getId() == null || currentClaim.getId() == null) {
                continue;
            }
            if (!c.getId().equals(currentClaim.getId()) && c.getClaimDate() != null) {
                long diffMs = Math.abs(now.getTime() - c.getClaimDate().getTime());
                return diffMs / (1000.0 * 60 * 60 * 24);
            }
        }
        return null;
    }
}

