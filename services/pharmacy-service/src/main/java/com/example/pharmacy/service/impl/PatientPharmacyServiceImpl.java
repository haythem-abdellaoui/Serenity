package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PatientDefaultPharmacyResponseDTO;
import com.example.pharmacy.dto.PharmacyCandidateResponseDTO;
import com.example.pharmacy.entity.PatientPharmacyPreference;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PatientPharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientPharmacyServiceImpl implements PatientPharmacyService {

    private final PatientPharmacyPreferenceRepository preferenceRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public PatientDefaultPharmacyResponseDTO getMyDefaultPharmacy() {
        Long patientId = currentUserService.getCurrentUserId();
        PatientPharmacyPreference preference = preferenceRepository.findByPatientId(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Default pharmacy preference", "patientId", patientId));

        return toDefaultResponse(preference);
    }

    @Override
    public PatientDefaultPharmacyResponseDTO setMyDefaultPharmacy(Long pharmacyId) {
        Long patientId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", pharmacyId));

        PatientPharmacyPreference preference = preferenceRepository.findByPatientId(patientId)
            .orElseGet(() -> PatientPharmacyPreference.builder().patientId(patientId).build());

        preference.setDefaultPharmacy(pharmacy);
        PatientPharmacyPreference savedPreference = preferenceRepository.save(preference);

        assignPendingUnassignedPrescriptions(patientId, pharmacy);
        return toDefaultResponse(savedPreference);
    }

    private void assignPendingUnassignedPrescriptions(Long patientId, Pharmacy pharmacy) {
        List<PharmacyPrescription> pendingWorkflows = pharmacyPrescriptionRepository
            .findByPatientIdAndAssignedPharmacyIsNullAndStatusOrderByCreatedAtDesc(patientId, PrescriptionStatus.PENDING);

        if (pendingWorkflows.isEmpty()) {
            return;
        }

        pendingWorkflows.forEach(workflow -> workflow.setAssignedPharmacy(pharmacy));
        pharmacyPrescriptionRepository.saveAll(pendingWorkflows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyCandidateResponseDTO> listPharmacies(String city, String governorate) {
        String cityFilter = normalize(city);
        String governorateFilter = normalize(governorate);

        return pharmacyRepository.findAllByOrderByNameAsc().stream()
            .filter(pharmacy -> matchesContains(pharmacy.getCity(), cityFilter))
            .filter(pharmacy -> matchesContains(pharmacy.getGovernorate(), governorateFilter))
            .map(pharmacy -> toCandidateResponse(pharmacy, null))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyCandidateResponseDTO> suggestNearest(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        double maxRadiusKm = (radiusKm == null || radiusKm <= 0) ? 20.0 : radiusKm;

        return pharmacyRepository.findAllByOrderByNameAsc().stream()
            .filter(pharmacy -> pharmacy.getLatitude() != null && pharmacy.getLongitude() != null)
            .map(pharmacy -> {
                double distance = calculateDistanceKm(latitude, longitude, pharmacy.getLatitude(), pharmacy.getLongitude());
                return toCandidateResponse(pharmacy, distance);
            })
            .filter(candidate -> candidate.getDistanceKm() != null && candidate.getDistanceKm() <= maxRadiusKm)
            .sorted(Comparator.comparingDouble(PharmacyCandidateResponseDTO::getDistanceKm))
            .limit(20)
            .toList();
    }

    private PatientDefaultPharmacyResponseDTO toDefaultResponse(PatientPharmacyPreference preference) {
        Pharmacy pharmacy = preference.getDefaultPharmacy();
        return PatientDefaultPharmacyResponseDTO.builder()
            .patientId(preference.getPatientId())
            .pharmacyId(pharmacy.getId())
            .pharmacyName(pharmacy.getName())
            .phone(pharmacy.getPhone())
            .openingHours(pharmacy.getOpeningHours())
            .addressLine(pharmacy.getAddressLine())
            .city(pharmacy.getCity())
            .governorate(pharmacy.getGovernorate())
            .latitude(pharmacy.getLatitude())
            .longitude(pharmacy.getLongitude())
            .supportsEmergency(pharmacy.getSupportsEmergency())
            .selectedAt(preference.getUpdatedAt() != null ? preference.getUpdatedAt().toString() : null)
            .build();
    }

    private PharmacyCandidateResponseDTO toCandidateResponse(Pharmacy pharmacy, Double distanceKm) {
        return PharmacyCandidateResponseDTO.builder()
            .id(pharmacy.getId())
            .name(pharmacy.getName())
            .phone(pharmacy.getPhone())
            .openingHours(pharmacy.getOpeningHours())
            .addressLine(pharmacy.getAddressLine())
            .city(pharmacy.getCity())
            .governorate(pharmacy.getGovernorate())
            .latitude(pharmacy.getLatitude())
            .longitude(pharmacy.getLongitude())
            .supportsEmergency(pharmacy.getSupportsEmergency())
            .distanceKm(distanceKm == null ? null : roundDistance(distanceKm))
            .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }

    private boolean matchesContains(String source, String filter) {
        if (filter == null) {
            return true;
        }
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(filter);
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private double roundDistance(double distance) {
        return Math.round(distance * 100.0) / 100.0;
    }
}
