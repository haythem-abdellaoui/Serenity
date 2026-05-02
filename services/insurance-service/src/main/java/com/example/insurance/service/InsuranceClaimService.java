package com.example.insurance.service;

import com.example.insurance.dto.ClaimRemittanceOcrSummaryDTO;
import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.dto.InsuranceClaimOcrAuditResponseDTO;
import com.example.insurance.dto.InsuranceClaimTransitionResponseDTO;
import com.example.insurance.dto.PageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface InsuranceClaimService {

    InsuranceClaimResponseDTO submitClaim(Long userId, InsuranceClaimRequestDTO request, List<MultipartFile> files);

    List<InsuranceClaimResponseDTO> getClaimsByUserId(Long userId);

    List<InsuranceClaimResponseDTO> getClaimsByUserId(
            Long userId,
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    );

    List<InsuranceClaimResponseDTO> getAllClaims();

    List<InsuranceClaimResponseDTO> getAllClaims(
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    );

    PageResponseDTO<InsuranceClaimResponseDTO> getClaimsByUserIdPaged(
            Long userId,
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            int page,
            int size
    );

    PageResponseDTO<InsuranceClaimResponseDTO> getAllClaimsPaged(
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            Long userId,
            int page,
            int size
    );

    InsuranceClaimResponseDTO getClaimById(Long id, Long requesterUserId, boolean isAdmin);

    InsuranceClaimResponseDTO approveClaim(Long id, Double montant, Long adminUserId);

    InsuranceClaimResponseDTO rejectClaim(Long id, Long adminUserId);

    void deleteClaim(Long id);

    InsuranceClaimResponseDTO requestAdditionalDocuments(Long claimId, Long adminUserId, String reason, Date deadline);

    InsuranceClaimResponseDTO submitAdditionalDocuments(
            Long claimId,
            Long userId,
            String message,
            String description,
            Double amount,
            Double insuranceGrade,
            List<MultipartFile> files
    );

    List<InsuranceClaimTransitionResponseDTO> getClaimTimeline(Long claimId, Long requesterUserId, boolean isAdmin);

    List<InsuranceClaimOcrAuditResponseDTO> getClaimOcrAudit(Long claimId, Long requesterUserId, boolean isAdmin);

    List<ClaimRemittanceOcrSummaryDTO> getRemittanceOcrSummaryReport();

    /**
     * For high-risk claims held in {@code UNDER_REVIEW}: manually forward the claim to the external insurer portal.
     */
    InsuranceClaimResponseDTO sendHeldClaimToPortal(Long claimId, Long adminUserId);

    /**
     * For high-risk claims held in {@code UNDER_REVIEW}: reject the claim internally (admin decision).
     */
    InsuranceClaimResponseDTO rejectHeldClaim(Long claimId, Long adminUserId, String reason);
}
