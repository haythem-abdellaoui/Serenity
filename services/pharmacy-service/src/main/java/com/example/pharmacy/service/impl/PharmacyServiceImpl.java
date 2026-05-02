package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PharmacyResponseDTO;
import com.example.pharmacy.dto.PharmacyUpsertRequestDTO;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyServiceImpl implements PharmacyService {

    private static final Set<PrescriptionStatus> RESET_TO_PENDING_STATUSES = Set.of(
        PrescriptionStatus.PENDING,
        PrescriptionStatus.ACCEPTED,
        PrescriptionStatus.READY_FOR_PICKUP
    );

    private final PharmacyRepository pharmacyRepository;
    private final MedicineStockItemRepository medicineStockItemRepository;
    private final PatientPharmacyPreferenceRepository patientPharmacyPreferenceRepository;
    private final PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    private final CurrentUserService currentUserService;

    @Override
    public PharmacyResponseDTO upsertMyPharmacy(PharmacyUpsertRequestDTO request) {
        Long userId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = pharmacyRepository.findByOwnerUserId(userId).orElseGet(Pharmacy::new);

        pharmacy.setOwnerUserId(userId);
        pharmacy.setName(request.getName());
        pharmacy.setLicenseNumber(request.getLicenseNumber());
        pharmacy.setPhone(request.getPhone());
        pharmacy.setOpeningHours(request.getOpeningHours());
        pharmacy.setAddressLine(request.getAddressLine());
        pharmacy.setCity(request.getCity());
        pharmacy.setGovernorate(request.getGovernorate());
        pharmacy.setLatitude(request.getLatitude());
        pharmacy.setLongitude(request.getLongitude());
        pharmacy.setSupportsEmergency(Boolean.TRUE.equals(request.getSupportsEmergency()));

        return toResponse(pharmacyRepository.save(pharmacy));
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyResponseDTO getMyPharmacy() {
        Long userId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = pharmacyRepository.findByOwnerUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "ownerUserId", userId));

        return toResponse(pharmacy);
    }

    @Override
    public void deleteMyPharmacy() {
        Long userId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = pharmacyRepository.findByOwnerUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "ownerUserId", userId));

        Long pharmacyId = pharmacy.getId();

        medicineStockItemRepository.deleteByPharmacyId(pharmacyId);
        patientPharmacyPreferenceRepository.deleteByDefaultPharmacy_Id(pharmacyId);

        List<PharmacyPrescription> assignedWorkflows = pharmacyPrescriptionRepository.findByAssignedPharmacyId(pharmacyId);
        if (!assignedWorkflows.isEmpty()) {
            assignedWorkflows.forEach(this::clearAssignedPharmacyAndResetWorkflow);
            pharmacyPrescriptionRepository.saveAll(assignedWorkflows);
        }

        pharmacyRepository.delete(pharmacy);
    }

    private void clearAssignedPharmacyAndResetWorkflow(PharmacyPrescription workflow) {
        workflow.setAssignedPharmacy(null);
        if (RESET_TO_PENDING_STATUSES.contains(workflow.getStatus())) {
            workflow.setStatus(PrescriptionStatus.PENDING);
            workflow.setReadyAt(null);
            workflow.setRejectionReason(null);
        }
    }

    private PharmacyResponseDTO toResponse(Pharmacy pharmacy) {
        return PharmacyResponseDTO.builder()
            .id(pharmacy.getId())
            .ownerUserId(pharmacy.getOwnerUserId())
            .name(pharmacy.getName())
            .licenseNumber(pharmacy.getLicenseNumber())
            .phone(pharmacy.getPhone())
            .openingHours(pharmacy.getOpeningHours())
            .addressLine(pharmacy.getAddressLine())
            .city(pharmacy.getCity())
            .governorate(pharmacy.getGovernorate())
            .latitude(pharmacy.getLatitude())
            .longitude(pharmacy.getLongitude())
            .supportsEmergency(pharmacy.getSupportsEmergency())
            .createdAt(pharmacy.getCreatedAt() != null ? pharmacy.getCreatedAt().toString() : null)
            .updatedAt(pharmacy.getUpdatedAt() != null ? pharmacy.getUpdatedAt().toString() : null)
            .build();
    }
}
