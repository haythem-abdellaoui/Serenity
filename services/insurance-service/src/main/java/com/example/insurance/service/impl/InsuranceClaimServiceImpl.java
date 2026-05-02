package com.example.insurance.service.impl;

import com.example.insurance.dto.ClaimRemittanceOcrSummaryDTO;
import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimOcrAuditResponseDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.dto.InsuranceClaimTransitionResponseDTO;
import com.example.insurance.dto.PageResponseDTO;
import com.example.insurance.dto.RemboursementResponseDTO;
import com.example.insurance.entity.InsuranceClaimOcrAudit;
import com.example.insurance.integration.InsurancePortalClient;
import com.example.insurance.integration.PortalResubmissionRequest;
import com.example.insurance.integration.PortalSubmitClaimRequest;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.InsuranceClaimTransition;
import com.example.insurance.entity.NotificationType;
import com.example.insurance.entity.OcrAnalysisDecision;
import com.example.insurance.entity.OcrAttemptType;
import com.example.insurance.entity.Remboursement;
import com.example.insurance.exception.OcrMajorMismatchException;
import com.example.insurance.ocr.ClaimConsistencyService;
import com.example.insurance.ocr.OcrConsistencyResult;
import com.example.insurance.ocr.OcrExtractionResult;
import com.example.insurance.ocr.OcrExtractionService;
import com.example.insurance.ocr.OcrMismatchDetail;
import com.example.insurance.ocr.OcrMismatchSeverity;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.InsuranceClaimOcrAuditRepository;
import com.example.insurance.repository.InsuranceClaimTransitionRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.ClaimRiskScoringService;
import com.example.insurance.service.InsuranceClaimService;
import com.example.insurance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceClaimServiceImpl implements InsuranceClaimService {
    private static final Set<String> ALLOWED_INSURANCE_COMPANIES = Set.of(
            "Insurance 1", "Insurance 2", "Insurance 3", "Insurance 4", "Insurance 5",
            "Insurance 6", "Insurance 7", "Insurance 8", "Insurance 9", "Insurance 10"
    );
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png"
    );
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final int FALLBACK_SAFE_JSON_LENGTH = 240;
    private static final int FALLBACK_SAFE_TEXT_LENGTH = 240;
    private static final double HIGH_RISK_SCORE_THRESHOLD = 70.0;

    private final InsuranceClaimRepository claimRepository;
    private final InsuranceClaimOcrAuditRepository ocrAuditRepository;
    private final InsuranceClaimTransitionRepository transitionRepository;
    private final RemboursementRepository remboursementRepository;
    private final InsurancePortalClient insurancePortalClient;
    private final NotificationService notificationService;
    private final OcrExtractionService ocrExtractionService;
    private final ClaimConsistencyService claimConsistencyService;
    private final ClaimRiskScoringService claimRiskScoringService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    @Value("${app.public-base-url:http://localhost:8090}")
    private String publicBaseUrl;
    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;
    @Value("${app.ocr.strict-major-block:true}")
    private boolean strictMajorBlock;
    @Value("${app.ocr.store-full-text:false}")
    private boolean storeFullExtractedText;
    @Value("${app.ocr.admin-alert-user-ids:}")
    private String ocrAdminAlertUserIds;

    @Override
    public InsuranceClaimResponseDTO submitClaim(Long userId, InsuranceClaimRequestDTO request, List<MultipartFile> files) {
        validateBusinessRules(request, files);

        List<String> filePaths = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            filePaths = saveFiles(files, userId);
        }

        OcrProcessingOutcome ocrOutcome = runOcrChecks(filePaths, request.getAmount(), request.getInsuranceCompany());
        OcrConsistencyResult ocrResult = ocrOutcome.result();
        if (ocrEnabled && strictMajorBlock && ocrResult.getSeverity() == OcrMismatchSeverity.MAJOR) {
            persistOcrAudit(
                    null,
                    userId,
                    OcrAttemptType.INITIAL_SUBMISSION,
                    OcrAnalysisDecision.MAJOR_BLOCKED,
                    request.getAmount(),
                    request.getInsuranceCompany(),
                    ocrResult,
                    ocrOutcome
            );
            notificationService.createNotification(
                    userId,
                    null,
                    NotificationType.OCR_MAJOR_BLOCKED,
                    "Claim blocked by document checks",
                    ocrResult.getMismatchSummary()
            );
            notifyAdminsSafely(
                    null,
                    NotificationType.OCR_MAJOR_BLOCKED,
                    "Admin alert: claim blocked by OCR",
                    "A claim submission was blocked due to major mismatches."
            );
            throw new OcrMajorMismatchException(
                    "Claim blocked due to major document mismatches. Please fix your form/documents and try again.",
                    ocrResult.getMismatches()
            );
        }

        String externalRef = UUID.randomUUID().toString();

        // Used to populate the DB column `reimbursement_amount` (non-null in schema)
        Double reimbursementAmount = calculateReimbursement(request.getAmount(), request.getInsuranceGrade());

        InsuranceClaim claim = InsuranceClaim.builder()
                .description(request.getDescription().trim())
                .amount(request.getAmount())
                .reimbursementAmount(reimbursementAmount)
                .insuranceCompany(request.getInsuranceCompany().trim())
                .insuranceGrade(request.getInsuranceGrade())
                .reason(null)
                .status(ClaimStatus.SUBMITTED)
                .externalRef(externalRef)
                .userId(userId)
                .filePaths(filePaths)
                .ocrLastDecision(ocrResult.getSeverity().name())
                .ocrMismatchCount(ocrResult.getMismatches() == null ? 0 : ocrResult.getMismatches().size())
                .ocrLastAnalyzedAt(new Date())
                .ocrSummary(ocrResult.getMismatchSummary())
                .build();

        if (ocrEnabled && ocrResult.getSeverity() == OcrMismatchSeverity.MINOR) {
            claim.setStatus(ClaimStatus.NEEDS_INFO);
            claim.setInfoRequestReason("OCR consistency check: " + ocrResult.getMismatchSummary());
            claim.setInfoRequestedAt(new Date());
            claim.setReason("Returned to patient for document/form correction.");
        }

        claimRepository.save(claim);
        persistOcrAudit(
                claim.getId(),
                userId,
                OcrAttemptType.INITIAL_SUBMISSION,
                ocrResult.getSeverity() == OcrMismatchSeverity.MINOR ? OcrAnalysisDecision.MINOR_MISMATCH : OcrAnalysisDecision.PASS,
                request.getAmount(),
                request.getInsuranceCompany(),
                ocrResult,
                ocrOutcome
        );

        if (ocrEnabled && ocrResult.getSeverity() == OcrMismatchSeverity.MINOR) {
            // Keep portal in sync with the claim reference even when local OCR asks the
            // patient for corrections; otherwise later resubmissions may not exist in portal.
            PortalSubmitClaimRequest portalReq = new PortalSubmitClaimRequest(
                    externalRef,
                    String.valueOf(userId),
                    request.getDescription(),
                    request.getAmount(),
                    reimbursementAmount,
                    request.getInsuranceCompany(),
                    request.getInsuranceGrade(),
                    buildAttachmentUrls(filePaths)
            );
            insurancePortalClient.submitClaim(portalReq);

            recordTransition(claim, null, ClaimStatus.NEEDS_INFO, userId, "SYSTEM", "OCR found minor mismatches");
            notificationService.createNotification(
                    userId,
                    claim.getId(),
                    NotificationType.OCR_MINOR_MISMATCH,
                    "Action needed on your claim",
                    "Your claim was returned for corrections: " + ocrResult.getMismatchSummary()
            );
            notifyAdminsSafely(
                    claim.getId(),
                    NotificationType.OCR_MINOR_MISMATCH,
                    "Admin alert: claim needs corrections",
                    "A claim has minor OCR mismatches and was returned to the patient."
            );
            return toResponseDTO(claim);
        }

        recordTransition(claim, null, ClaimStatus.SUBMITTED, userId, "USER", "Claim submitted");

        // High-risk hold: if risk score is 70+ we stop here for admin review
        try {
            var score = claimRiskScoringService.scoreClaim(claim.getId());
            if (score != null && score.getRiskScore() != null && score.getRiskScore() >= HIGH_RISK_SCORE_THRESHOLD) {
                ClaimStatus fromStatus = claim.getStatus();
                claim.setStatus(ClaimStatus.UNDER_REVIEW);
                claim.setReason("Held for admin review due to high risk score (" + score.getRiskScore() + ").");
                claimRepository.save(claim);
                recordTransition(
                        claim,
                        fromStatus,
                        ClaimStatus.UNDER_REVIEW,
                        userId,
                        "SYSTEM",
                        "High risk score: " + score.getRiskScore()
                );
                notifyAdminsSafely(
                        claim.getId(),
                        NotificationType.OCR_MINOR_MISMATCH,
                        "Admin review required: high-risk claim",
                        "Claim " + claim.getExternalRef() + " was held for review (risk score " + score.getRiskScore() + ")."
                );
                notificationService.createNotification(
                        userId,
                        claim.getId(),
                        NotificationType.DOCUMENTS_REQUESTED,
                        "Claim pending manual review",
                        "Your claim requires a manual review before being sent to the insurer."
                );
                return toResponseDTO(claim);
            }
        } catch (Exception ignored) {
            // If risk service is unavailable, keep default behavior and submit to portal.
        }

        // Fire-and-forget: external provider may accept/reject asynchronously.
        // Our scheduler will poll portal status and update this claim.
        PortalSubmitClaimRequest portalReq = new PortalSubmitClaimRequest(
                externalRef,
                String.valueOf(userId), // portal displays patientName; we only have userId here
                request.getDescription(),
                request.getAmount(),
                reimbursementAmount,
                request.getInsuranceCompany(),
                request.getInsuranceGrade(),
                buildAttachmentUrls(filePaths)
        );
        insurancePortalClient.submitClaim(portalReq);

        notificationService.createNotification(
                userId,
                claim.getId(),
                NotificationType.CLAIM_SENT_TO_INSURER,
                "Claim sent to insurer",
                "Your claim has been sent to the external insurance portal and is awaiting their decision."
        );

        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO sendHeldClaimToPortal(Long claimId, Long adminUserId) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + claimId));

        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only UNDER_REVIEW claims can be sent to portal");
        }

        PortalSubmitClaimRequest portalReq = new PortalSubmitClaimRequest(
                claim.getExternalRef(),
                String.valueOf(claim.getUserId()),
                claim.getDescription(),
                claim.getAmount(),
                claim.getReimbursementAmount(),
                claim.getInsuranceCompany(),
                claim.getInsuranceGrade(),
                buildAttachmentUrls(claim.getFilePaths())
        );
        insurancePortalClient.submitClaim(portalReq);

        ClaimStatus from = claim.getStatus();
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setReason("Sent to insurer portal after admin review.");
        claimRepository.save(claim);
        recordTransition(claim, from, ClaimStatus.SUBMITTED, adminUserId, "ADMIN", "Sent to insurer portal");

        notificationService.createNotification(
                claim.getUserId(),
                claim.getId(),
                NotificationType.CLAIM_SENT_TO_INSURER,
                "Claim sent to insurer",
                "Your claim was reviewed and sent to the external insurance portal."
        );
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO rejectHeldClaim(Long claimId, Long adminUserId, String reason) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + claimId));

        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only UNDER_REVIEW claims can be rejected by admin");
        }

        ClaimStatus from = claim.getStatus();
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setReason(reason == null || reason.isBlank() ? "Rejected by admin review." : reason.trim());
        claimRepository.save(claim);
        recordTransition(claim, from, ClaimStatus.REJECTED, adminUserId, "ADMIN", claim.getReason());

        notificationService.createNotification(
                claim.getUserId(),
                claim.getId(),
                NotificationType.CLAIM_REJECTED,
                "Claim rejected",
                "Your claim was rejected after an admin review."
        );
        return toResponseDTO(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getClaimsByUserId(Long userId) {
        return getClaimsByUserId(userId, null, null, null, null, "claimDate", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getClaimsByUserId(
            Long userId,
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        return findClaims(userId, status, insuranceCompany, fromDate, toDate, sortBy, sortDir)
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getAllClaims() {
        return getAllClaims(null, null, null, null, "claimDate", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getAllClaims(
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        return findClaims(null, status, insuranceCompany, fromDate, toDate, sortBy, sortDir)
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<InsuranceClaimResponseDTO> getClaimsByUserIdPaged(
            Long userId,
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        Page<InsuranceClaimResponseDTO> dtoPage = findClaimsPaged(
                userId, status, insuranceCompany, fromDate, toDate, sortBy, sortDir, null, page, size
        ).map(this::toResponseDTO);
        return PageResponseDTO.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<InsuranceClaimResponseDTO> getAllClaimsPaged(
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            Long userId,
            int page,
            int size
    ) {
        Page<InsuranceClaimResponseDTO> dtoPage = findClaimsPaged(
                null, status, insuranceCompany, fromDate, toDate, sortBy, sortDir, userId, page, size
        ).map(this::toResponseDTO);
        return PageResponseDTO.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceClaimResponseDTO getClaimById(Long id, Long requesterUserId, boolean isAdmin) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
        if (!isAdmin && !claim.getUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this claim");
        }
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO requestAdditionalDocuments(Long claimId, Long adminUserId, String reason, Date deadline) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + claimId));
        ensureTransitionAllowed(claim.getStatus(), ClaimStatus.NEEDS_INFO);

        ClaimStatus fromStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.NEEDS_INFO);
        claim.setInfoRequestReason(reason == null ? null : reason.trim());
        claim.setInfoRequestDeadline(deadline);
        claim.setInfoRequestedAt(new Date());
        claim.setInfoRespondedAt(null);
        claimRepository.save(claim);
        recordTransition(claim, fromStatus, ClaimStatus.NEEDS_INFO, adminUserId, "ADMIN", reason);

        notificationService.createNotification(
                claim.getUserId(),
                claim.getId(),
                NotificationType.DOCUMENTS_REQUESTED,
                "Additional documents required",
                "Your claim requires additional documents before it can be processed."
        );
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO submitAdditionalDocuments(
            Long claimId,
            Long userId,
            String message,
            String description,
            Double amount,
            Double insuranceGrade,
            List<MultipartFile> files
    ) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + claimId));
        if (!claim.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update this claim");
        }
        if (claim.getStatus() != ClaimStatus.NEEDS_INFO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Claim is not waiting for additional documents");
        }
        if (claim.getInfoRequestDeadline() != null && claim.getInfoRequestDeadline().before(new Date())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document submission deadline has passed");
        }
        validateAdditionalFiles(files);

        boolean hasChanges = false;
        if (description != null) {
            String trimmed = description.trim();
            if (trimmed.length() < 10 || trimmed.length() > 500) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must be between 10 and 500 characters");
            }
            if (!trimmed.equals(claim.getDescription())) {
                claim.setDescription(trimmed);
                hasChanges = true;
            }
        }
        if (amount != null) {
            if (amount <= 0 || amount > 100000.0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be between 0.01 and 100000");
            }
            if (!amount.equals(claim.getAmount())) {
                claim.setAmount(amount);
                hasChanges = true;
            }
        }
        if (insuranceGrade != null) {
            if (insuranceGrade < 1.0 || insuranceGrade > 5.0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insurance grade must be between 1 and 5");
            }
            if (!insuranceGrade.equals(claim.getInsuranceGrade())) {
                claim.setInsuranceGrade(insuranceGrade);
                hasChanges = true;
            }
        }

        List<String> newFiles = saveFiles(files == null ? List.of() : files, userId);
        if (!newFiles.isEmpty()) {
            List<String> merged = new ArrayList<>(claim.getFilePaths() == null ? List.of() : claim.getFilePaths());
            merged.addAll(newFiles);
            claim.setFilePaths(merged);
            hasChanges = true;
        }

        String normalizedMessage = message == null ? "" : message.trim();
        if (!normalizedMessage.isEmpty()) {
            hasChanges = true;
        }
        if (!hasChanges) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No changes detected. Update at least one field, add a message, or attach a document."
            );
        }

        OcrProcessingOutcome ocrOutcome = runOcrChecks(claim.getFilePaths(), claim.getAmount(), claim.getInsuranceCompany());
        OcrConsistencyResult ocrResult = ocrOutcome.result();
        claim.setOcrLastDecision(ocrResult.getSeverity().name());
        claim.setOcrMismatchCount(ocrResult.getMismatches() == null ? 0 : ocrResult.getMismatches().size());
        claim.setOcrLastAnalyzedAt(new Date());
        claim.setOcrSummary(ocrResult.getMismatchSummary());
        persistOcrAudit(
                claim.getId(),
                userId,
                OcrAttemptType.RESUBMISSION,
                ocrResult.getSeverity() == OcrMismatchSeverity.MAJOR
                        ? OcrAnalysisDecision.MAJOR_BLOCKED
                        : (ocrResult.getSeverity() == OcrMismatchSeverity.MINOR ? OcrAnalysisDecision.MINOR_MISMATCH : OcrAnalysisDecision.PASS),
                claim.getAmount(),
                claim.getInsuranceCompany(),
                ocrResult,
                ocrOutcome
        );

        if (ocrEnabled && strictMajorBlock && ocrResult.getSeverity() == OcrMismatchSeverity.MAJOR) {
            claimRepository.save(claim);
            notificationService.createNotification(
                    claim.getUserId(),
                    claim.getId(),
                    NotificationType.OCR_MAJOR_BLOCKED,
                    "Resubmission blocked by document checks",
                    ocrResult.getMismatchSummary()
            );
            notifyAdminsSafely(
                    claim.getId(),
                    NotificationType.OCR_MAJOR_BLOCKED,
                    "Admin alert: resubmission blocked by OCR",
                    "A claim resubmission was blocked due to major mismatches."
            );
            throw new OcrMajorMismatchException(
                    "Resubmission blocked due to major document mismatches. Please fix your form/documents and try again.",
                    ocrResult.getMismatches()
            );
        }

        if (ocrEnabled && ocrResult.getSeverity() == OcrMismatchSeverity.MINOR) {
            claim.setStatus(ClaimStatus.NEEDS_INFO);
            claim.setInfoRequestReason("OCR consistency check: " + ocrResult.getMismatchSummary());
            claim.setInfoRequestedAt(new Date());
            claim.setInfoRespondedAt(null);
            claimRepository.save(claim);
            recordTransition(
                    claim,
                    ClaimStatus.NEEDS_INFO,
                    ClaimStatus.NEEDS_INFO,
                    userId,
                    "SYSTEM",
                    "OCR returned claim for additional correction"
            );
            notificationService.createNotification(
                    claim.getUserId(),
                    claim.getId(),
                    NotificationType.OCR_MINOR_MISMATCH,
                    "More corrections needed",
                    "OCR detected minor mismatches: " + ocrResult.getMismatchSummary()
            );
            notifyAdminsSafely(
                    claim.getId(),
                    NotificationType.OCR_MINOR_MISMATCH,
                    "Admin alert: claim still needs correction",
                    "A resubmitted claim still has minor OCR mismatches."
            );
            return toResponseDTO(claim);
        }

        ClaimStatus fromStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setInfoRespondedAt(new Date());
        claim.setReimbursementAmount(calculateReimbursement(claim.getAmount(), claim.getInsuranceGrade()));
        claimRepository.save(claim);
        if (claim.getExternalRef() != null && !claim.getExternalRef().isBlank()) {
            insurancePortalClient.notifyResubmission(
                    claim.getExternalRef(),
                    new PortalResubmissionRequest(
                            claim.getDescription(),
                            claim.getAmount(),
                            claim.getReimbursementAmount(),
                            claim.getInsuranceGrade(),
                            normalizedMessage.isEmpty() ? null : normalizedMessage,
                            buildAttachmentUrls(claim.getFilePaths())
                    )
            );
        }
        recordTransition(
                claim,
                fromStatus,
                ClaimStatus.UNDER_REVIEW,
                userId,
                "USER",
                normalizedMessage.isEmpty() ? "Patient revised claim and submitted requested documents" : normalizedMessage
        );

        notificationService.createNotification(
                claim.getUserId(),
                claim.getId(),
                NotificationType.DOCUMENTS_SUBMITTED,
                "Additional documents submitted",
                "Your additional documents have been received and your claim is now under review."
        );
        return toResponseDTO(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimTransitionResponseDTO> getClaimTimeline(Long claimId, Long requesterUserId, boolean isAdmin) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + claimId));
        if (!isAdmin && !claim.getUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this claim timeline");
        }
        return transitionRepository.findByInsuranceClaimIdOrderByChangedAtAsc(claimId)
                .stream()
                .map(this::toTransitionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimOcrAuditResponseDTO> getClaimOcrAudit(Long claimId, Long requesterUserId, boolean isAdmin) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + claimId));
        if (!isAdmin && !claim.getUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this OCR audit");
        }
        return ocrAuditRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(this::toOcrAuditDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimRemittanceOcrSummaryDTO> getRemittanceOcrSummaryReport() {
        return claimRepository.findRemittanceOcrSummaryByJpql();
    }

    @Override
    public InsuranceClaimResponseDTO approveClaim(Long id, Double montant, Long adminUserId) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
        ensureTransitionAllowed(claim.getStatus(), ClaimStatus.APPROVED);
        ClaimStatus fromStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setReimbursementAmount(montant);
        claim.setReason(null);
        Remboursement remboursement = Remboursement.builder()
                .montant(montant)
                .statut(ClaimStatus.APPROVED)
                .insuranceClaim(claim)
                .build();
        remboursementRepository.save(remboursement);
        claimRepository.save(claim);
        recordTransition(claim, fromStatus, ClaimStatus.APPROVED, adminUserId, "ADMIN", "Claim approved");
        notificationService.createNotification(
                claim.getUserId(),
                claim.getId(),
                NotificationType.CLAIM_APPROVED,
                "Claim approved",
                "Your claim has been approved."
        );
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO rejectClaim(Long id, Long adminUserId) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
        ensureTransitionAllowed(claim.getStatus(), ClaimStatus.REJECTED);
        ClaimStatus fromStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setReimbursementAmount(0.0);
        claim.setReason(null);
        claimRepository.save(claim);
        recordTransition(claim, fromStatus, ClaimStatus.REJECTED, adminUserId, "ADMIN", "Claim rejected");
        notificationService.createNotification(
                claim.getUserId(),
                claim.getId(),
                NotificationType.CLAIM_REJECTED,
                "Claim rejected",
                "Your claim has been rejected."
        );
        return toResponseDTO(claim);
    }

    @Override
    public void deleteClaim(Long id) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
        if (claim.getStatus() != ClaimStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only rejected claims can be deleted");
        }

        claimRepository.delete(claim);
    }

    private List<String> saveFiles(List<MultipartFile> files, Long userId) {
        List<String> paths = new ArrayList<>();
        try {
            Path uploadPath = Paths.get(uploadDir, "claims", userId.toString());
            Files.createDirectories(uploadPath);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                file.transferTo(filePath.toAbsolutePath().toFile());
                paths.add(filePath.toString().replace("\\", "/"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload files", e);
        }
        return paths;
    }

    private void validateBusinessRules(InsuranceClaimRequestDTO request, List<MultipartFile> files) {
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        if (request.getInsuranceCompany() == null || !ALLOWED_INSURANCE_COMPANIES.contains(request.getInsuranceCompany().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insurance company is invalid");
        }
        if (request.getInsuranceGrade() == null || request.getInsuranceGrade() < 1.0 || request.getInsuranceGrade() > 5.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insurance grade must be between 1 and 5");
        }

        if (files == null || files.isEmpty()) {
            return;
        }
        if (files.size() > MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 files are allowed");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each file must be <= 10 MB");
            }
            String type = file.getContentType();
            if (type == null || !ALLOWED_FILE_TYPES.contains(type.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPG and PNG files are allowed");
            }
        }
    }

    private void validateAdditionalFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each file must be <= 10 MB");
            }
            String type = file.getContentType();
            if (type == null || !ALLOWED_FILE_TYPES.contains(type.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPG and PNG files are allowed");
            }
        }
    }

    private OcrProcessingOutcome runOcrChecks(List<String> filePaths, Double amount, String insuranceCompany) {
        if (!ocrEnabled || filePaths == null || filePaths.isEmpty()) {
            OcrConsistencyResult passResult = OcrConsistencyResult.builder()
                    .severity(OcrMismatchSeverity.NONE)
                    .mismatches(List.of())
                    .mismatchSummary("OCR checks skipped: no attachments or OCR disabled.")
                    .build();
            return new OcrProcessingOutcome("", passResult);
        }
        OcrExtractionResult extractionResult = ocrExtractionService.extractFromFiles(filePaths);
        OcrConsistencyResult consistencyResult = claimConsistencyService.analyze(
                extractionResult.getCombinedText(),
                amount,
                insuranceCompany
        );
        return new OcrProcessingOutcome(extractionResult.getCombinedText(), consistencyResult);
    }

    private void persistOcrAudit(
            Long claimId,
            Long userId,
            OcrAttemptType attemptType,
            OcrAnalysisDecision decision,
            Double submittedAmount,
            String submittedCompany,
            OcrConsistencyResult result,
            OcrProcessingOutcome outcome
    ) {
        int mismatchCount = result.getMismatches() == null ? 0 : result.getMismatches().size();
        int majorCount = result.getMismatches() == null ? 0 : (int) result.getMismatches().stream()
                .filter(m -> m.getSeverity() == OcrMismatchSeverity.MAJOR)
                .count();
        int minorCount = result.getMismatches() == null ? 0 : (int) result.getMismatches().stream()
                .filter(m -> m.getSeverity() == OcrMismatchSeverity.MINOR)
                .count();

        InsuranceClaimOcrAudit audit = InsuranceClaimOcrAudit.builder()
                .claimId(claimId)
                .userId(userId)
                .attemptType(attemptType)
                .decision(decision)
                .submittedAmount(submittedAmount)
                .submittedCompany(submittedCompany)
                .extractedAmount(result.getExtractedAmount())
                .extractedInvoiceDate(result.getExtractedInvoiceDate())
                .extractedProviderName(result.getExtractedProviderName())
                .mismatchCount(mismatchCount)
                .majorCount(majorCount)
                .minorCount(minorCount)
                .mismatchSummary(result.getMismatchSummary())
                .mismatchDetailsJson(toMismatchJson(result.getMismatches()))
                .extractedText(storeFullExtractedText && outcome != null ? outcome.extractedText() : null)
                .build();
        try {
            ocrAuditRepository.save(audit);
        } catch (DataIntegrityViolationException ex) {
            // Backward-compatible fallback for pre-existing narrow DB columns.
            audit.setMismatchDetailsJson(truncate(audit.getMismatchDetailsJson(), FALLBACK_SAFE_JSON_LENGTH));
            audit.setExtractedText(truncate(audit.getExtractedText(), FALLBACK_SAFE_TEXT_LENGTH));
            audit.setMismatchSummary(truncate(audit.getMismatchSummary(), 500));
            ocrAuditRepository.save(audit);
        }
    }

    private String toMismatchJson(List<OcrMismatchDetail> mismatches) {
        if (mismatches == null || mismatches.isEmpty()) {
            return "[]";
        }
        return mismatches.stream()
                .map(m -> "{"
                        + "\"code\":\"" + safeJson(m.getCode()) + "\","
                        + "\"field\":\"" + safeJson(m.getField()) + "\","
                        + "\"expected\":\"" + safeJson(m.getExpectedValue()) + "\","
                        + "\"extracted\":\"" + safeJson(m.getExtractedValue()) + "\","
                        + "\"severity\":\"" + (m.getSeverity() == null ? "" : m.getSeverity().name()) + "\","
                        + "\"message\":\"" + safeJson(m.getMessage()) + "\""
                        + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void notifyAdminsSafely(Long claimId, NotificationType type, String title, String message) {
        List<Long> adminIds = parseAdminAlertUserIds();
        for (Long adminId : adminIds) {
            try {
                notificationService.createNotification(adminId, claimId, type, title, message);
            } catch (Exception ignored) {
                // Admin alert delivery should never block patient claim flow.
            }
        }
    }

    private List<Long> parseAdminAlertUserIds() {
        if (ocrAdminAlertUserIds == null || ocrAdminAlertUserIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ocrAdminAlertUserIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::safeParseLong)
                .filter(v -> v != null && v > 0)
                .toList();
    }

    private Long safeParseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private InsuranceClaimResponseDTO toResponseDTO(InsuranceClaim claim) {
        List<RemboursementResponseDTO> rembDtos = claim.getRemboursements() != null
                ? claim.getRemboursements().stream()
                .map(r -> RemboursementResponseDTO.builder()
                        .id(r.getId())
                        .montant(r.getMontant())
                        .date(r.getDate())
                        .statut(r.getStatut().name())
                        .claimId(claim.getId())
                        .build())
                .toList()
                : List.of();

        return InsuranceClaimResponseDTO.builder()
                .id(claim.getId())
                .description(claim.getDescription())
                .claimDate(claim.getClaimDate())
                .amount(claim.getAmount())
                .reimbursementAmount(claim.getReimbursementAmount())
                .insuranceCompany(claim.getInsuranceCompany())
                .insuranceGrade(claim.getInsuranceGrade())
                .reason(claim.getReason())
                .status(claim.getStatus().name())
                .externalRef(claim.getExternalRef())
                .filePaths(claim.getFilePaths())
                .userId(claim.getUserId())
                .infoRequestReason(claim.getInfoRequestReason())
                .infoRequestDeadline(claim.getInfoRequestDeadline())
                .infoRequestedAt(claim.getInfoRequestedAt())
                .infoRespondedAt(claim.getInfoRespondedAt())
                .ocrLastDecision(claim.getOcrLastDecision())
                .ocrMismatchCount(claim.getOcrMismatchCount())
                .ocrLastAnalyzedAt(claim.getOcrLastAnalyzedAt())
                .ocrSummary(claim.getOcrSummary())
                .remboursements(rembDtos)
                .build();
    }

    private InsuranceClaimOcrAuditResponseDTO toOcrAuditDto(InsuranceClaimOcrAudit audit) {
        return InsuranceClaimOcrAuditResponseDTO.builder()
                .id(audit.getId())
                .claimId(audit.getClaimId())
                .userId(audit.getUserId())
                .attemptType(audit.getAttemptType().name())
                .decision(audit.getDecision().name())
                .submittedAmount(audit.getSubmittedAmount())
                .submittedCompany(audit.getSubmittedCompany())
                .extractedAmount(audit.getExtractedAmount())
                .extractedInvoiceDate(audit.getExtractedInvoiceDate())
                .extractedProviderName(audit.getExtractedProviderName())
                .mismatchCount(audit.getMismatchCount())
                .majorCount(audit.getMajorCount())
                .minorCount(audit.getMinorCount())
                .mismatchSummary(audit.getMismatchSummary())
                .mismatchDetailsJson(audit.getMismatchDetailsJson())
                .createdAt(audit.getCreatedAt())
                .build();
    }

    private record OcrProcessingOutcome(String extractedText, OcrConsistencyResult result) {}

    private InsuranceClaimTransitionResponseDTO toTransitionDto(InsuranceClaimTransition transition) {
        return InsuranceClaimTransitionResponseDTO.builder()
                .id(transition.getId())
                .fromStatus(transition.getFromStatus() == null ? null : transition.getFromStatus().name())
                .toStatus(transition.getToStatus().name())
                .changedByUserId(transition.getChangedByUserId())
                .changedByRole(transition.getChangedByRole())
                .reason(transition.getReason())
                .changedAt(transition.getChangedAt())
                .build();
    }

    private void recordTransition(
            InsuranceClaim claim,
            ClaimStatus fromStatus,
            ClaimStatus toStatus,
            Long changedByUserId,
            String changedByRole,
            String reason
    ) {
        InsuranceClaimTransition transition = InsuranceClaimTransition.builder()
                .insuranceClaim(claim)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedByUserId(changedByUserId)
                .changedByRole(changedByRole)
                .reason(reason)
                .build();
        transitionRepository.save(transition);
    }

    private void ensureTransitionAllowed(ClaimStatus from, ClaimStatus to) {
        if (from == null) {
            return;
        }
        if (from == to) {
            return;
        }
        boolean allowed;
        switch (from) {
            case PENDING:
            case SUBMITTED:
                allowed = to == ClaimStatus.UNDER_REVIEW
                        || to == ClaimStatus.NEEDS_INFO
                        || to == ClaimStatus.APPROVED
                        || to == ClaimStatus.PARTIALLY_APPROVED
                        || to == ClaimStatus.REJECTED;
                break;
            case UNDER_REVIEW:
                allowed = to == ClaimStatus.SUBMITTED
                        || to == ClaimStatus.NEEDS_INFO
                        || to == ClaimStatus.APPROVED
                        || to == ClaimStatus.PARTIALLY_APPROVED
                        || to == ClaimStatus.REJECTED;
                break;
            case NEEDS_INFO:
                allowed = to == ClaimStatus.UNDER_REVIEW || to == ClaimStatus.REJECTED;
                break;
            case APPROVED:
            case PARTIALLY_APPROVED:
                allowed = to == ClaimStatus.PAID;
                break;
            case PAID:
            case REJECTED:
                allowed = false;
                break;
            default:
                allowed = false;
        }
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition: " + from + " -> " + to);
        }
    }

    private Double calculateReimbursement(Double amount, Double insuranceGrade) {
        if (amount == null || insuranceGrade == null) {
            return 0.0;
        }
        int grade = insuranceGrade.intValue();
        // Keep in sync with Angular `INSURANCE_GRADES` percentages
        int percentage;
        switch (grade) {
            case 1 -> percentage = 10;
            case 2 -> percentage = 12;
            case 3 -> percentage = 18;
            case 4 -> percentage = 25;
            case 5 -> percentage = 45;
            default -> percentage = 0;
        }
        return Math.round((amount * percentage / 100.0) * 100.0) / 100.0;
    }

    private List<String> buildAttachmentUrls(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>(filePaths.size());
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        for (String path : filePaths) {
            String encoded = URLEncoder.encode(path, StandardCharsets.UTF_8);
            urls.add(base + "/api/files/open?path=" + encoded);
        }
        return urls;
    }

    private List<InsuranceClaim> findClaims(
            Long userId,
            String statusRaw,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortByRaw,
            String sortDirRaw
    ) {
        Sort sort = buildSort(sortByRaw, sortDirRaw);
        Specification<InsuranceClaim> spec = buildClaimSpecification(userId, statusRaw, insuranceCompany, fromDate, toDate, null);
        return claimRepository.findAll(spec, sort);
    }

    private Page<InsuranceClaim> findClaimsPaged(
            Long userId,
            String statusRaw,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortByRaw,
            String sortDirRaw,
            Long adminUserFilterId,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Sort sort = buildSort(sortByRaw, sortDirRaw);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);
        Specification<InsuranceClaim> spec = buildClaimSpecification(
                userId, statusRaw, insuranceCompany, fromDate, toDate, adminUserFilterId
        );
        return claimRepository.findAll(spec, pageable);
    }

    private Specification<InsuranceClaim> buildClaimSpecification(
            Long userId,
            String statusRaw,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            Long adminUserFilterId
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate cannot be after toDate");
        }

        ClaimStatus status = parseStatus(statusRaw);
        Specification<InsuranceClaim> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (adminUserFilterId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), adminUserFilterId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (insuranceCompany != null && !insuranceCompany.isBlank()) {
            String lowered = insuranceCompany.trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("insuranceCompany")), "%" + lowered + "%"));
        }
        if (fromDate != null) {
            var fromInstant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("claimDate"), java.util.Date.from(fromInstant)));
        }
        if (toDate != null) {
            var toInstant = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("claimDate"), java.util.Date.from(toInstant)));
        }
        return spec;
    }

    private ClaimStatus parseStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            return null;
        }
        try {
            return ClaimStatus.valueOf(statusRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + statusRaw);
        }
    }

    private Sort buildSort(String sortByRaw, String sortDirRaw) {
        String sortBy = sortByRaw == null ? "claimDate" : sortByRaw.trim();
        String field = switch (sortBy.toLowerCase()) {
            case "date", "claimdate" -> "claimDate";
            case "amount" -> "amount";
            case "reimbursement", "reimbursementamount" -> "reimbursementAmount";
            case "status" -> "status";
            case "insurancecompany", "company" -> "insuranceCompany";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy: " + sortByRaw);
        };

        String sortDir = sortDirRaw == null ? "desc" : sortDirRaw.trim().toLowerCase();
        Sort.Direction direction = switch (sortDir) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortDir: " + sortDirRaw);
        };
        return Sort.by(direction, field);
    }
}
