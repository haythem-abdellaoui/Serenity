package com.example.insurance.repository;

import com.example.insurance.entity.Remboursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemboursementRepository extends JpaRepository<Remboursement, Long> {

    List<Remboursement> findByInsuranceClaimId(Long claimId);
}
