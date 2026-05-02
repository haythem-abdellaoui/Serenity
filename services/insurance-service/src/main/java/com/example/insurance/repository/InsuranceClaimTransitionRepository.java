package com.example.insurance.repository;

import com.example.insurance.entity.InsuranceClaimTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceClaimTransitionRepository extends JpaRepository<InsuranceClaimTransition, Long> {
    List<InsuranceClaimTransition> findByInsuranceClaimIdOrderByChangedAtAsc(Long claimId);
}
