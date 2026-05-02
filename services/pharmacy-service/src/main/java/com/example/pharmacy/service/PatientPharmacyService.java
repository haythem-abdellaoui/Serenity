package com.example.pharmacy.service;

import com.example.pharmacy.dto.PatientDefaultPharmacyResponseDTO;
import com.example.pharmacy.dto.PharmacyCandidateResponseDTO;

import java.util.List;

public interface PatientPharmacyService {
    PatientDefaultPharmacyResponseDTO getMyDefaultPharmacy();
    PatientDefaultPharmacyResponseDTO setMyDefaultPharmacy(Long pharmacyId);
    List<PharmacyCandidateResponseDTO> listPharmacies(String city, String governorate);
    List<PharmacyCandidateResponseDTO> suggestNearest(Double latitude, Double longitude, Double radiusKm);
}
