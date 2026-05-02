package com.example.insurance.repository;

import com.example.insurance.entity.InsuranceClaimOcrAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceClaimOcrAuditRepository extends JpaRepository<InsuranceClaimOcrAudit, Long> {
    List<InsuranceClaimOcrAudit> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
