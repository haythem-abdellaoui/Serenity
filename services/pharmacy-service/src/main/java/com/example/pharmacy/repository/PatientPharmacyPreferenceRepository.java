package com.example.pharmacy.repository;

import com.example.pharmacy.entity.PatientPharmacyPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientPharmacyPreferenceRepository extends JpaRepository<PatientPharmacyPreference, Long> {
    Optional<PatientPharmacyPreference> findByPatientId(Long patientId);

    long deleteByDefaultPharmacy_Id(Long defaultPharmacyId);
}
