package com.example.insurance.integration;

import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.InsuranceClaimTransition;
import com.example.insurance.entity.NotificationType;
import com.example.insurance.entity.Remboursement;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.InsuranceClaimTransitionRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InsuranceStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(InsuranceStatusPoller.class);

    private final InsuranceClaimRepository claimRepository;
    private final InsuranceClaimTransitionRepository transitionRepository;
    private final RemboursementRepository remboursementRepository;
    private final InsurancePortalClient insurancePortalClient;
    private final NotificationService notificationService;

    @Transactional
    @Scheduled(fixedDelayString = "${app.status-poller.fixed-delay-ms}")
    public void pollPendingClaims() {
        List<InsuranceClaim> pendingClaims = claimRepository.findByStatusInOrderByClaimDateDesc(List.of(
                ClaimStatus.PENDING,
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW
        ));
        if (pendingClaims.isEmpty()) {
            return;
        }

        for (InsuranceClaim claim : pendingClaims) {
            String externalRef = claim.getExternalRef();
            if (externalRef == null || externalRef.isBlank()) {
                continue;
            }

            PortalClaimStatusResponse status = insurancePortalClient.fetchClaimStatus(externalRef);
            if (status == null || status.getStatus() == null) {
                continue;
            }

            String portalStatus = status.getStatus().trim().toUpperCase();
            if ("APPROVED".equals(portalStatus)) {
                Double amount = status.getReimbursementAmount() != null ? status.getReimbursementAmount() : 0.0;
                ClaimStatus fromStatus = claim.getStatus();
                claim.setStatus(ClaimStatus.APPROVED);
                claim.setReimbursementAmount(amount);
                claim.setReason(status.getReason());

                Remboursement remboursement = Remboursement.builder()
                        .montant(amount)
                        .statut(ClaimStatus.APPROVED)
                        .insuranceClaim(claim)
                        .build();
                remboursementRepository.save(remboursement);
                claimRepository.save(claim);
                transitionRepository.save(InsuranceClaimTransition.builder()
                        .insuranceClaim(claim)
                        .fromStatus(fromStatus)
                        .toStatus(ClaimStatus.APPROVED)
                        .changedByUserId(0L)
                        .changedByRole("SYSTEM")
                        .reason("Updated from external insurer status poller")
                        .build());
                notificationService.createNotification(
                        claim.getUserId(),
                        claim.getId(),
                        NotificationType.CLAIM_APPROVED,
                        "External insurer approved your claim",
                        "Your claim was approved by the insurance portal with reimbursement amount: " + amount
                                + (status.getReason() != null && !status.getReason().isBlank() ? ". Reason: " + status.getReason() : "")
                );
                log.info("Updated claim {} from portal status APPROVED", externalRef);
            } else if ("NEEDS_INFO".equals(portalStatus)) {
                // Avoid loop: if patient already responded and claim is currently under review,
                // do not force it back to NEEDS_INFO unless external status genuinely changed later.
                if (claim.getStatus() == ClaimStatus.UNDER_REVIEW
                        && claim.getInfoRespondedAt() != null
                        && (claim.getInfoRequestedAt() == null || claim.getInfoRespondedAt().after(claim.getInfoRequestedAt()))) {
                    continue;
                }
                ClaimStatus fromStatus = claim.getStatus();
                claim.setStatus(ClaimStatus.NEEDS_INFO);
                claim.setInfoRequestReason(status.getReason());
                if (status.getInfoRequestDeadline() != null && !status.getInfoRequestDeadline().isBlank()) {
                    try {
                        claim.setInfoRequestDeadline(java.util.Date.from(Instant.parse(status.getInfoRequestDeadline())));
                    } catch (Exception ignored) {
                        // Keep null deadline when format is invalid.
                    }
                }
                claim.setInfoRequestedAt(new java.util.Date());
                claimRepository.save(claim);
                transitionRepository.save(InsuranceClaimTransition.builder()
                        .insuranceClaim(claim)
                        .fromStatus(fromStatus)
                        .toStatus(ClaimStatus.NEEDS_INFO)
                        .changedByUserId(0L)
                        .changedByRole("SYSTEM")
                        .reason(status.getReason() == null ? "Additional documents requested by external insurer" : status.getReason())
                        .build());
                notificationService.createNotification(
                        claim.getUserId(),
                        claim.getId(),
                        NotificationType.DOCUMENTS_REQUESTED,
                        "External insurer requested more documents",
                        "Your claim requires additional documents."
                                + (status.getReason() != null && !status.getReason().isBlank() ? " Reason: " + status.getReason() : "")
                );
                log.info("Updated claim {} from portal status NEEDS_INFO", externalRef);
            } else if ("REJECTED".equals(portalStatus)) {
                ClaimStatus fromStatus = claim.getStatus();
                claim.setStatus(ClaimStatus.REJECTED);
                // DB schema requires a non-null reimbursement_amount
                claim.setReimbursementAmount(0.0);
                claim.setReason(status.getReason());
                // If remboursements exist, clear them (orphanRemoval should handle)
                if (claim.getRemboursements() != null) {
                    claim.getRemboursements().clear();
                }
                claimRepository.save(claim);
                transitionRepository.save(InsuranceClaimTransition.builder()
                        .insuranceClaim(claim)
                        .fromStatus(fromStatus)
                        .toStatus(ClaimStatus.REJECTED)
                        .changedByUserId(0L)
                        .changedByRole("SYSTEM")
                        .reason("Updated from external insurer status poller")
                        .build());
                notificationService.createNotification(
                        claim.getUserId(),
                        claim.getId(),
                        NotificationType.CLAIM_REJECTED,
                        "External insurer rejected your claim",
                        "Your claim was rejected by the insurance portal."
                                + (status.getReason() != null && !status.getReason().isBlank() ? " Reason: " + status.getReason() : "")
                );
                log.info("Updated claim {} from portal status REJECTED", externalRef);
            }
        }
    }
}

