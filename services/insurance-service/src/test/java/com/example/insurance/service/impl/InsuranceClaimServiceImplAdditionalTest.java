package com.example.insurance.service.impl;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceClaimServiceImplAdditionalTest {

    @Mock private InsuranceClaimRepository claimRepository;
    @Mock private InsuranceClaimOcrAuditRepository ocrAuditRepository;
    @Mock private InsuranceClaimTransitionRepository transitionRepository;
    @Mock private RemboursementRepository remboursementRepository;
    @Mock private InsurancePortalClient insurancePortalClient;
    @Mock private NotificationService notificationService;
    @Mock private OcrExtractionService ocrExtractionService;
    @Mock private ClaimConsistencyService claimConsistencyService;
    @Mock private ClaimRiskScoringService claimRiskScoringService;

    @InjectMocks private InsuranceClaimServiceImpl service;

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
    void submitClaim_whenMissingDescription_throwsBadRequest() {
        InsuranceClaimRequestDTO req = InsuranceClaimRequestDTO.builder()
                .description("  ")
                .amount(100.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.submitClaim(7L, req, List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void sendHeldClaimToPortal_whenNotUnderReview_throwsBadRequest() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(7L)
                .status(ClaimStatus.SUBMITTED)
                .externalRef("ref")
                .description("d")
                .amount(10.0)
                .reimbursementAmount(1.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .filePaths(List.of())
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.sendHeldClaimToPortal(1L, 99L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(insurancePortalClient, never()).submitClaim(any());
    }

    @Test
    void rejectHeldClaim_whenNotUnderReview_throwsBadRequest() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(7L)
                .status(ClaimStatus.SUBMITTED)
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.rejectHeldClaim(1L, 99L, "no"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void requestAdditionalDocuments_invalidTransition_throwsBadRequest() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(7L)
                .status(ClaimStatus.REJECTED)
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.requestAdditionalDocuments(1L, 99L, "need", new Date())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void submitAdditionalDocuments_whenNotOwner_throwsForbidden() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(777L)
                .status(ClaimStatus.NEEDS_INFO)
                .description("Some valid description")
                .amount(10.0)
                .reimbursementAmount(0.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .filePaths(List.of())
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.submitAdditionalDocuments(1L, 7L, "", null, null, null, List.of())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void submitAdditionalDocuments_whenWrongStatus_throwsBadRequest() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(7L)
                .status(ClaimStatus.SUBMITTED)
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.submitAdditionalDocuments(1L, 7L, "", null, null, null, List.of())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void submitAdditionalDocuments_whenDeadlinePassed_throwsBadRequest() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(7L)
                .status(ClaimStatus.NEEDS_INFO)
                .infoRequestDeadline(new Date(System.currentTimeMillis() - 60_000))
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.submitAdditionalDocuments(1L, 7L, "", null, null, null, List.of())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void submitAdditionalDocuments_whenNoChangesDetected_throwsBadRequest() {
        InsuranceClaim claim = InsuranceClaim.builder()
                .id(1L)
                .userId(7L)
                .status(ClaimStatus.NEEDS_INFO)
                .description("Some valid description")
                .amount(10.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .filePaths(List.of())
                .build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.submitAdditionalDocuments(1L, 7L, "   ", null, null, null, List.of())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}

