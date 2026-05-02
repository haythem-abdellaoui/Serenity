package com.example.insurance.controller;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimOcrAuditResponseDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.dto.InsuranceClaimTransitionResponseDTO;
import com.example.insurance.dto.PageResponseDTO;
import com.example.insurance.dto.RequestAdditionalDocumentsDTO;
import com.example.insurance.dto.SubmitAdditionalDocumentsDTO;
import com.example.insurance.dto.ClaimRemittanceOcrSummaryDTO;
import com.example.insurance.dto.ClaimRiskScoreResponseDTO;
import com.example.insurance.security.InsuranceAuth;
import com.example.insurance.service.ClaimRiskScoringService;
import com.example.insurance.service.InsuranceClaimService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
@Validated
public class InsuranceClaimController {

    private final InsuranceClaimService insuranceClaimService;
    private final ClaimRiskScoringService claimRiskScoringService;

    @PostMapping(value = "/claims", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InsuranceClaimResponseDTO> submitClaim(
            @Valid @ModelAttribute InsuranceClaimRequestDTO request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(insuranceClaimService.submitClaim(userId, request, files));
    }

    @GetMapping("/claims/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getMyClaims(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String insuranceCompany,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "claimDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(insuranceClaimService.getClaimsByUserId(
                userId, status, insuranceCompany, fromDate, toDate, sortBy, sortDir
        ));
    }

    @GetMapping("/claims/me/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponseDTO<InsuranceClaimResponseDTO>> getMyClaimsPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String insuranceCompany,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "claimDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "12") @Min(1) Integer size
    ) {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(insuranceClaimService.getClaimsByUserIdPaged(
                userId, status, insuranceCompany, fromDate, toDate, sortBy, sortDir, page, size
        ));
    }

    @GetMapping("/claims")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getAllClaims(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String insuranceCompany,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "claimDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(insuranceClaimService.getAllClaims(
                status, insuranceCompany, fromDate, toDate, sortBy, sortDir
        ));
    }

    @GetMapping("/reports/remittance-ocr-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimRemittanceOcrSummaryDTO>> getRemittanceOcrSummaryReport() {
        return ResponseEntity.ok(insuranceClaimService.getRemittanceOcrSummaryReport());
    }

    @GetMapping("/claims/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<InsuranceClaimResponseDTO>> getAllClaimsPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String insuranceCompany,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "claimDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "12") @Min(1) Integer size
    ) {
        return ResponseEntity.ok(insuranceClaimService.getAllClaimsPaged(
                status, insuranceCompany, fromDate, toDate, sortBy, sortDir, userId, page, size
        ));
    }

    @GetMapping("/claims/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InsuranceClaimResponseDTO> getClaimById(@PathVariable Long id) {
        Long userId = InsuranceAuth.requireUserId();
        boolean admin = InsuranceAuth.isAdmin();
        return ResponseEntity.ok(insuranceClaimService.getClaimById(id, userId, admin));
    }

    @GetMapping("/claims/{id}/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InsuranceClaimTransitionResponseDTO>> getClaimTimeline(@PathVariable Long id) {
        Long userId = InsuranceAuth.requireUserId();
        boolean admin = InsuranceAuth.isAdmin();
        return ResponseEntity.ok(insuranceClaimService.getClaimTimeline(id, userId, admin));
    }

    @GetMapping("/claims/{id}/ocr-audit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InsuranceClaimOcrAuditResponseDTO>> getClaimOcrAudit(@PathVariable Long id) {
        Long userId = InsuranceAuth.requireUserId();
        boolean admin = InsuranceAuth.isAdmin();
        return ResponseEntity.ok(insuranceClaimService.getClaimOcrAudit(id, userId, admin));
    }

    @GetMapping("/claims/{id}/risk-score")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClaimRiskScoreResponseDTO> getRiskScore(@PathVariable Long id) {
        return ResponseEntity.ok(claimRiskScoringService.scoreClaim(id));
    }

    @PostMapping("/claims/{id}/send-to-portal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> sendHeldClaimToPortal(@PathVariable Long id) {
        Long adminUserId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(insuranceClaimService.sendHeldClaimToPortal(id, adminUserId));
    }

    @PostMapping("/claims/{id}/reject-held")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> rejectHeldClaim(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Long adminUserId = InsuranceAuth.requireUserId();
        String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
        return ResponseEntity.ok(insuranceClaimService.rejectHeldClaim(id, adminUserId, reason));
    }

    @PostMapping("/claims/{id}/request-documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> requestAdditionalDocuments(
            @PathVariable Long id,
            @Valid @RequestBody RequestAdditionalDocumentsDTO request
    ) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Admin is read-only in this app. Claim decisions are managed by the external insurer portal."
        );
    }

    @PostMapping(value = "/claims/{id}/documents-response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InsuranceClaimResponseDTO> submitAdditionalDocuments(
            @PathVariable Long id,
            @Valid @ModelAttribute SubmitAdditionalDocumentsDTO request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(insuranceClaimService.submitAdditionalDocuments(
                id,
                userId,
                request.getMessage(),
                request.getDescription(),
                request.getAmount(),
                request.getInsuranceGrade(),
                files
        ));
    }

    @PatchMapping("/claims/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> approveClaim(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0") Double montant) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Admin is read-only in this app. Claim decisions are managed by the external insurer portal."
        );
    }

    @PatchMapping("/claims/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> rejectClaim(@PathVariable Long id) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Admin is read-only in this app. Claim decisions are managed by the external insurer portal."
        );
    }

    @DeleteMapping("/claims/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        insuranceClaimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }
}
