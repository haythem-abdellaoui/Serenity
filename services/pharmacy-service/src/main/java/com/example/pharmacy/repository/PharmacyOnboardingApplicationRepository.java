package com.example.pharmacy.repository;

import com.example.pharmacy.entity.PharmacyApplicationStatus;
import com.example.pharmacy.entity.PharmacyOnboardingApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PharmacyOnboardingApplicationRepository extends JpaRepository<PharmacyOnboardingApplication, Long> {

    Optional<PharmacyOnboardingApplication> findByUserId(Long userId);

    List<PharmacyOnboardingApplication> findAllByStatusOrderBySubmittedAtDesc(PharmacyApplicationStatus status);

    List<PharmacyOnboardingApplication> findAllByOrderBySubmittedAtDesc();

    Optional<PharmacyOnboardingApplication> findFirstByCnopNumberIgnoreCaseAndStatusIn(
        String cnopNumber,
        Collection<PharmacyApplicationStatus> statuses
    );

    Optional<PharmacyOnboardingApplication> findFirstByAuthorizationReferenceNumberIgnoreCaseAndStatusIn(
        String authorizationReferenceNumber,
        Collection<PharmacyApplicationStatus> statuses
    );
}
