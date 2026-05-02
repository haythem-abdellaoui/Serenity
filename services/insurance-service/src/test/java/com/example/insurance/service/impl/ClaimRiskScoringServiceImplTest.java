package com.example.insurance.service.impl;

import com.example.insurance.dto.ClaimRiskScoreResponseDTO;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.InsuranceClaimOcrAudit;
import com.example.insurance.entity.OcrAnalysisDecision;
import com.example.insurance.entity.OcrAttemptType;
import com.example.insurance.integration.ClaimRiskModelClient;
import com.example.insurance.integration.ClaimRiskModelScoreResponse;
import com.example.insurance.repository.InsuranceClaimOcrAuditRepository;
import com.example.insurance.repository.InsuranceClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimRiskScoringServiceImplTest {

    @Mock
    private InsuranceClaimRepository claimRepository;

    @Mock
    private InsuranceClaimOcrAuditRepository ocrAuditRepository;

    @Mock
    private ClaimRiskModelClient claimRiskModelClient;

    @InjectMocks
    private ClaimRiskScoringServiceImpl service;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    @Test
    void scoreClaim_shouldBuildPayloadAndReturnDto() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(100L)
                .userId(7L)
                .amount(123.45)
                .reimbursementAmount(12.34)
                .insuranceCompany("Insurance 3")
                .insuranceGrade(3.0)
                .filePaths(List.of("a", "b"))
                .status(ClaimStatus.SUBMITTED)
                .description("d")
                .build();

        InsuranceClaim prev = InsuranceClaim.builder()
                .id(99L)
                .userId(7L)
                .claimDate(new Date(System.currentTimeMillis() - 5L * 24L * 60L * 60L * 1000L))
                .amount(1.0)
                .reimbursementAmount(0.0)
                .insuranceCompany("Insurance 3")
                .insuranceGrade(3.0)
                .status(ClaimStatus.SUBMITTED)
                .description("p")
                .build();

        InsuranceClaimOcrAudit audit = InsuranceClaimOcrAudit.builder()
                .claimId(100L)
                .userId(7L)
                .attemptType(OcrAttemptType.INITIAL_SUBMISSION)
                .decision(OcrAnalysisDecision.PASS)
                .mismatchCount(2)
                .majorCount(1)
                .minorCount(1)
                .submittedAmount(123.45)
                .extractedAmount(120.0)
                .build();

        ClaimRiskModelScoreResponse modelResp = new ClaimRiskModelScoreResponse();
        modelResp.setRiskScore(77.0);
        modelResp.setRiskBand("HIGH");
        modelResp.setTopReasons(List.of("r1", "r2"));

        when(claimRepository.findById(100L)).thenReturn(Optional.of(claim));
        when(ocrAuditRepository.findByClaimIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(audit));
        when(claimRepository.countByUserId(7L)).thenReturn(10L);
        when(claimRepository.countByUserIdAndStatus(7L, ClaimStatus.REJECTED)).thenReturn(3L);
        when(claimRepository.countByUserIdAndClaimDateAfter(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(4L);
        when(claimRepository.findByUserIdOrderByClaimDateDesc(7L)).thenReturn(List.of(claim, prev));
        when(claimRiskModelClient.score(org.mockito.ArgumentMatchers.anyMap())).thenReturn(modelResp);

        ClaimRiskScoreResponseDTO dto = service.scoreClaim(100L);

        assertNotNull(dto);
        assertEquals(77.0, dto.getRiskScore());
        assertEquals("HIGH", dto.getRiskBand());
        assertEquals(List.of("r1", "r2"), dto.getTopReasons());

        verify(claimRiskModelClient).score(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(100L, payload.get("claim_id"));
        assertEquals(7L, payload.get("user_id"));
        assertEquals(123.45, (Double) payload.get("amount"), 0.0001);
        assertEquals(12.34, (Double) payload.get("reimbursement_amount"), 0.0001);
        assertEquals("Insurance 3", payload.get("insurance_company"));
        assertEquals(3.0, (Double) payload.get("insurance_grade"), 0.0001);
        assertEquals(2, payload.get("file_count"));
        assertEquals("PASS", payload.get("ocr_decision"));
        assertEquals(2, payload.get("ocr_mismatch_count"));
        assertEquals(1, payload.get("ocr_major_count"));
        assertEquals(1, payload.get("ocr_minor_count"));
        assertNotNull(payload.get("days_since_last_claim"));
    }

    @Test
    void scoreClaim_shouldThrowNotFoundWhenClaimMissing() {
        when(claimRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.scoreClaim(1L));
    }

    @Test
    void scoreClaim_shouldThrowServiceUnavailableWhenModelReturnsNull() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(100L)
                .userId(7L)
                .amount(10.0)
                .reimbursementAmount(1.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .filePaths(List.of())
                .status(ClaimStatus.SUBMITTED)
                .description("d")
                .build();
        when(claimRepository.findById(100L)).thenReturn(Optional.of(claim));
        when(ocrAuditRepository.findByClaimIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());
        when(claimRepository.countByUserId(7L)).thenReturn(0L);
        when(claimRepository.countByUserIdAndStatus(7L, ClaimStatus.REJECTED)).thenReturn(0L);
        when(claimRepository.countByUserIdAndClaimDateAfter(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(0L);
        when(claimRepository.findByUserIdOrderByClaimDateDesc(7L)).thenReturn(List.of(claim));
        when(claimRiskModelClient.score(org.mockito.ArgumentMatchers.anyMap())).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> service.scoreClaim(100L));
    }
}

