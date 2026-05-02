package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PatientDefaultPharmacyResponseDTO;
import com.example.pharmacy.entity.PatientPharmacyPreference;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientPharmacyServiceImplTest {

    @Mock
    private PatientPharmacyPreferenceRepository preferenceRepository;
    @Mock
    private PharmacyRepository pharmacyRepository;
    @Mock
    private PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PatientPharmacyServiceImpl patientPharmacyService;

    @Test
    void setMyDefaultPharmacy_assignsPendingUnassignedPrescriptions() {
        Long patientId = 77L;
        Long pharmacyId = 8L;
        Pharmacy pharmacy = Pharmacy.builder().id(pharmacyId).name("City Pharmacy").build();

        PharmacyPrescription pendingA = PharmacyPrescription.builder()
            .id(1L)
            .patientId(patientId)
            .status(PrescriptionStatus.PENDING)
            .build();
        PharmacyPrescription pendingB = PharmacyPrescription.builder()
            .id(2L)
            .patientId(patientId)
            .status(PrescriptionStatus.PENDING)
            .build();

        when(currentUserService.getCurrentUserId()).thenReturn(patientId);
        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(preferenceRepository.findByPatientId(patientId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(PatientPharmacyPreference.class))).thenAnswer(invocation -> {
            PatientPharmacyPreference saved = invocation.getArgument(0);
            saved.setId(50L);
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });
        when(pharmacyPrescriptionRepository.findByPatientIdAndAssignedPharmacyIsNullAndStatusOrderByCreatedAtDesc(patientId, PrescriptionStatus.PENDING))
            .thenReturn(List.of(pendingA, pendingB));

        PatientDefaultPharmacyResponseDTO response = patientPharmacyService.setMyDefaultPharmacy(pharmacyId);

        assertEquals(patientId, response.getPatientId());
        assertEquals(pharmacyId, response.getPharmacyId());
        assertEquals(pharmacy, pendingA.getAssignedPharmacy());
        assertEquals(pharmacy, pendingB.getAssignedPharmacy());
        verify(pharmacyPrescriptionRepository).saveAll(List.of(pendingA, pendingB));
    }

    @Test
    void setMyDefaultPharmacy_doesNotSaveWorkflowsWhenNoPendingUnassignedExist() {
        Long patientId = 77L;
        Long pharmacyId = 8L;
        Pharmacy pharmacy = Pharmacy.builder().id(pharmacyId).name("City Pharmacy").build();

        PatientPharmacyPreference existingPreference = PatientPharmacyPreference.builder()
            .id(10L)
            .patientId(patientId)
            .defaultPharmacy(pharmacy)
            .build();

        when(currentUserService.getCurrentUserId()).thenReturn(patientId);
        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(preferenceRepository.findByPatientId(patientId)).thenReturn(Optional.of(existingPreference));
        when(preferenceRepository.save(any(PatientPharmacyPreference.class))).thenAnswer(invocation -> {
            PatientPharmacyPreference saved = invocation.getArgument(0);
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });
        when(pharmacyPrescriptionRepository.findByPatientIdAndAssignedPharmacyIsNullAndStatusOrderByCreatedAtDesc(patientId, PrescriptionStatus.PENDING))
            .thenReturn(List.of());

        patientPharmacyService.setMyDefaultPharmacy(pharmacyId);

        verify(pharmacyPrescriptionRepository, never()).saveAll(any());
    }
}
