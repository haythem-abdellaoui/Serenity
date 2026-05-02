package com.example.insurance.service.impl;

import com.example.insurance.dto.ClaimRiskScoreResponseDTO;
import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.integration.InsurancePortalClient;
import com.example.insurance.ocr.ClaimConsistencyService;
import com.example.insurance.ocr.OcrExtractionService;
import com.example.insurance.repository.InsuranceClaimOcrAuditRepository;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.InsuranceClaimTransitionRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.ClaimRiskScoringService;
import com.example.insurance.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceClaimServiceImplTest {

    @Mock
    private InsuranceClaimRepository claimRepository;
    @Mock
    private InsuranceClaimOcrAuditRepository ocrAuditRepository;
    @Mock
    private InsuranceClaimTransitionRepository transitionRepository;
    @Mock
    private RemboursementRepository remboursementRepository;
    @Mock
    private InsurancePortalClient insurancePortalClient;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OcrExtractionService ocrExtractionService;
    @Mock
    private ClaimConsistencyService claimConsistencyService;
    @Mock
    private ClaimRiskScoringService claimRiskScoringService;

    @InjectMocks
    private InsuranceClaimServiceImpl service;

    @Captor
    private ArgumentCaptor<InsuranceClaim> claimCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "uploadDir", "build/test-uploads");
        ReflectionTestUtils.setField(service, "publicBaseUrl", "http://localhost:8090");
        ReflectionTestUtils.setField(service, "ocrEnabled", false);
        ReflectionTestUtils.setField(service, "strictMajorBlock", true);
        ReflectionTestUtils.setField(service, "storeFullExtractedText", false);
        ReflectionTestUtils.setField(service, "ocrAdminAlertUserIds", "");
    }

    @Test
    void submitClaim_shouldPersistClaimAndSubmitToPortal_whenOcrDisabledAndNoFiles() {
        InsuranceClaimRequestDTO req = InsuranceClaimRequestDTO.builder()
                .description("Some valid description")
                .amount(100.0)
                .insuranceCompany("Insurance 3")
                .insuranceGrade(3.0)
                .build();

        when(claimRiskScoringService.scoreClaim(any())).thenReturn(ClaimRiskScoreResponseDTO.builder().riskScore(10.0).build());
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(inv -> {
            InsuranceClaim c = inv.getArgument(0);
            c.setId(123L);
            return c;
        });

        var dto = service.submitClaim(7L, req, List.of());

        assertNotNull(dto);
        assertEquals(123L, dto.getId());
        assertEquals(ClaimStatus.SUBMITTED.name(), dto.getStatus());

        verify(claimRepository).save(claimCaptor.capture());
        InsuranceClaim saved = claimCaptor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals(100.0, saved.getAmount(), 0.0001);
        assertEquals("Insurance 3", saved.getInsuranceCompany());
        assertNotNull(saved.getExternalRef());
        assertEquals(ClaimStatus.SUBMITTED, saved.getStatus());

        verify(insurancePortalClient).submitClaim(any());
        verify(notificationService).createNotification(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(123L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(transitionRepository).save(any());
        verify(ocrAuditRepository).save(any());
    }

    @Test
    void submitClaim_shouldRejectInvalidInsuranceCompany() {
        InsuranceClaimRequestDTO req = InsuranceClaimRequestDTO.builder()
                .description("Some valid description")
                .amount(100.0)
                .insuranceCompany("Not a valid company")
                .insuranceGrade(3.0)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.submitClaim(7L, req, List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void getClaimById_shouldForbidNonAdminAccessToOtherUsersClaim() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(10L)
                .userId(99L)
                .description("Some valid description")
                .amount(10.0)
                .reimbursementAmount(0.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .status(ClaimStatus.SUBMITTED)
                .externalRef("ref")
                .claimDate(new Date())
                .build();
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getClaimById(10L, 7L, false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteClaim_shouldOnlyAllowRejectedClaims() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(10L)
                .userId(7L)
                .description("Some valid description")
                .amount(10.0)
                .reimbursementAmount(0.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .status(ClaimStatus.SUBMITTED)
                .externalRef("ref")
                .claimDate(new Date())
                .build();
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.deleteClaim(10L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(claimRepository, never()).delete(any(InsuranceClaim.class));
    }
}

