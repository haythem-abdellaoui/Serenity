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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceImplTest {

    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private MedicineStockItemRepository medicineStockItemRepository;
    @Mock private PatientPharmacyPreferenceRepository patientPharmacyPreferenceRepository;
    @Mock private PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks private PharmacyServiceImpl service;

    @Test
    void getMyPharmacy_whenMissing_throws() {
        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(pharmacyRepository.findByOwnerUserId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyPharmacy())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void upsertMyPharmacy_setsOwnerAndSaves() {
        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(pharmacyRepository.findByOwnerUserId(9L)).thenReturn(Optional.empty());

        Pharmacy saved = new Pharmacy();
        saved.setId(1L);
        saved.setOwnerUserId(9L);
        saved.setName("N");
        when(pharmacyRepository.save(any(Pharmacy.class))).thenReturn(saved);

        PharmacyUpsertRequestDTO req = new PharmacyUpsertRequestDTO();
        req.setName("N");
        req.setLicenseNumber("L");
        req.setSupportsEmergency(true);

        PharmacyResponseDTO dto = service.upsertMyPharmacy(req);
        assertThat(dto.getOwnerUserId()).isEqualTo(9L);
    }

    @Test
    void deleteMyPharmacy_clearsAssignedWorkflowsAndResetsPendingStatuses() {
        when(currentUserService.getCurrentUserId()).thenReturn(9L);

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(3L);
        pharmacy.setOwnerUserId(9L);
        when(pharmacyRepository.findByOwnerUserId(9L)).thenReturn(Optional.of(pharmacy));

        PharmacyPrescription w1 = new PharmacyPrescription();
        w1.setAssignedPharmacy(pharmacy);
        w1.setStatus(PrescriptionStatus.ACCEPTED);
        w1.setReadyAt(LocalDateTime.now());
        w1.setRejectionReason("x");

        PharmacyPrescription w2 = new PharmacyPrescription();
        w2.setAssignedPharmacy(pharmacy);
        w2.setStatus(PrescriptionStatus.COLLECTED);

        when(pharmacyPrescriptionRepository.findByAssignedPharmacyId(3L)).thenReturn(List.of(w1, w2));

        service.deleteMyPharmacy();

        assertThat(w1.getAssignedPharmacy()).isNull();
        assertThat(w1.getStatus()).isEqualTo(PrescriptionStatus.PENDING);
        assertThat(w1.getReadyAt()).isNull();
        assertThat(w1.getRejectionReason()).isNull();

        assertThat(w2.getAssignedPharmacy()).isNull();
        assertThat(w2.getStatus()).isEqualTo(PrescriptionStatus.COLLECTED);

        verify(medicineStockItemRepository).deleteByPharmacyId(3L);
        verify(patientPharmacyPreferenceRepository).deleteByDefaultPharmacy_Id(3L);
        verify(pharmacyPrescriptionRepository).saveAll(List.of(w1, w2));
        verify(pharmacyRepository).delete(pharmacy);
    }
}

