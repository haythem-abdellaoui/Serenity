package com.example.insurance.repository;

import com.example.insurance.dto.ClaimRemittanceOcrSummaryDTO;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long>, JpaSpecificationExecutor<InsuranceClaim> {

    List<InsuranceClaim> findByUserIdOrderByClaimDateDesc(Long userId);

    List<InsuranceClaim> findAllByOrderByClaimDateDesc();

    List<InsuranceClaim> findByStatusOrderByClaimDateDesc(ClaimStatus status);

    List<InsuranceClaim> findByStatusInOrderByClaimDateDesc(List<ClaimStatus> statuses);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ClaimStatus status);

    long countByUserIdAndClaimDateAfter(Long userId, Date since);

    @Query("""
            SELECT new com.example.insurance.dto.ClaimRemittanceOcrSummaryDTO(
                c.id,
                c.externalRef,
                c.userId,
                c.status,
                c.amount,
                c.reimbursementAmount,
                c.insuranceCompany,
                c.claimDate,
                COALESCE(SUM(r.montant), 0.0),
                COUNT(r.id),
                (SELECT COUNT(a.id) FROM InsuranceClaimOcrAudit a WHERE a.claimId = c.id)
            )
            FROM InsuranceClaim c
            LEFT JOIN c.remboursements r
            GROUP BY c.id, c.externalRef, c.userId, c.status, c.amount, c.reimbursementAmount, c.insuranceCompany, c.claimDate
            ORDER BY c.claimDate DESC
            """)
    List<ClaimRemittanceOcrSummaryDTO> findRemittanceOcrSummaryByJpql();
}
