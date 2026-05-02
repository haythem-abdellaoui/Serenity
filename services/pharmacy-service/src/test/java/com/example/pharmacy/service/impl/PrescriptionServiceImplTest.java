package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PrescriptionAlternativeResponseDTO;
import com.example.pharmacy.dto.PrescriptionLineResponseDTO;
import com.example.pharmacy.dto.PrescriptionPharmacyReassignRequestDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.entity.StockState;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.repository.StockConsumptionEventRepository;
import com.example.pharmacy.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock
    private PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    @Mock
    private PharmacyRepository pharmacyRepository;
    @Mock
    private MedicineStockItemRepository medicineStockItemRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PrescriptionLineQueryService prescriptionLineQueryService;
    @Mock
    private PrescriptionAlternativeService prescriptionAlternativeService;
    @Mock
    private PrescriptionResponseMapper prescriptionResponseMapper;
    @Mock
    private PrescriptionInsuranceDocumentStorageService prescriptionInsuranceDocumentStorageService;
    @Mock
    private StockConsumptionEventRepository stockConsumptionEventRepository;

    @Spy
    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    @Test
    void reassignPatientPrescriptionPharmacy_rejectsPharmacyOutsideRecommendedSet() {
        Long patientId = 201L;
        PharmacyPrescription workflow = pendingWorkflow(52L, 5001L, patientId, null);

        PrescriptionAlternativeResponseDTO alternatives = PrescriptionAlternativeResponseDTO.builder()
            .selectablePharmacies(List.of())
            .build();

        when(currentUserService.getCurrentUserId()).thenReturn(patientId);
        when(pharmacyPrescriptionRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        doReturn(alternatives).when(prescriptionService).getPatientAlternatives(workflow.getId(), 36.8, 10.17);
        when(prescriptionAlternativeService.extractSelectablePharmacyIds(alternatives)).thenReturn(Set.of());

        PrescriptionPharmacyReassignRequestDTO request = PrescriptionPharmacyReassignRequestDTO.builder()
            .pharmacyId(999L)
            .latitude(36.8)
            .longitude(10.17)
            .build();

        assertThrows(IllegalArgumentException.class,
            () -> prescriptionService.reassignPatientPrescriptionPharmacy(workflow.getId(), request));
    }

    @Test
    void updatePrescriptionStatus_happyPath_acceptsPendingPrescription() {
        Pharmacy pharmacy = pharmacy(9L, 501L, "Main", "L1");
        PharmacyPrescription workflow = pendingWorkflow(3L, 3003L, 200L, pharmacy);
        PrescriptionLineResponseDTO line = line(2L, "Ibuprofen", "2x", 1);
        PrescriptionResponseDTO mapped = PrescriptionResponseDTO.builder()
            .id(3L)
            .status(PrescriptionStatus.ACCEPTED)
            .medicineLines(List.of(line))
            .build();

        when(currentUserService.getCurrentUserId()).thenReturn(501L);
        when(pharmacyPrescriptionRepository.findById(3L)).thenReturn(Optional.of(workflow));
        when(pharmacyPrescriptionRepository.save(workflow)).thenReturn(workflow);
        when(prescriptionLineQueryService.loadLinesForSource(3003L)).thenReturn(List.of(line));
        when(prescriptionResponseMapper.toResponse(workflow, List.of(line))).thenReturn(mapped);

        PrescriptionStatusUpdateRequestDTO request = PrescriptionStatusUpdateRequestDTO.builder()
            .status(PrescriptionStatus.ACCEPTED)
            .build();

        PrescriptionResponseDTO result = prescriptionService.updatePrescriptionStatus(3L, request);

        assertEquals(PrescriptionStatus.ACCEPTED, result.getStatus());
        assertEquals(PrescriptionStatus.ACCEPTED, workflow.getStatus());
        verify(pharmacyPrescriptionRepository).save(workflow);
    }

    @Test
    void updatePrescriptionStatus_readyForPickup_throwsOnInsufficientStock() {
        Pharmacy pharmacy = pharmacy(12L, 501L, "Main", "L12");
        PharmacyPrescription workflow = PharmacyPrescription.builder()
            .id(4L)
            .sourcePrescriptionId(4004L)
            .assignedPharmacy(pharmacy)
            .doctorId(100L)
            .patientId(200L)
            .doctorNameSnapshot("Doctor")
            .patientNameSnapshot("Patient")
            .status(PrescriptionStatus.ACCEPTED)
            .createdAt(LocalDateTime.now())
            .build();

        PrescriptionLineQueryService.RequiredLine requiredLine =
            new PrescriptionLineQueryService.RequiredLine(1L, "Panadol", "panadol", 5);
        PrescriptionLineResponseDTO line = line(1L, "Panadol", "1x", 5);

        MedicineStockItem stock = MedicineStockItem.builder()
            .id(1L)
            .pharmacy(pharmacy)
            .medicineName("Panadol")
            .quantity(2)
            .state(StockState.IN_STOCK)
            .archived(false)
            .build();

        when(currentUserService.getCurrentUserId()).thenReturn(501L);
        when(pharmacyPrescriptionRepository.findById(4L)).thenReturn(Optional.of(workflow));
        when(prescriptionLineQueryService.loadLinesForSource(4004L)).thenReturn(List.of(line));
        when(prescriptionLineQueryService.resolveRequiredLines(List.of(line))).thenReturn(List.of(requiredLine));
        when(medicineStockItemRepository.findActiveByPharmacyIdsAndMedicineNames(eq(Set.of(12L)), eq(Set.of("panadol"))))
            .thenReturn(List.of(stock));

        PrescriptionStatusUpdateRequestDTO request = PrescriptionStatusUpdateRequestDTO.builder()
            .status(PrescriptionStatus.READY_FOR_PICKUP)
            .build();

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> prescriptionService.updatePrescriptionStatus(4L, request)
        );

        assertEquals(
            "Cannot mark prescription as ready. Insufficient stock for Panadol (required: 5, available: 2)",
            ex.getMessage()
        );
        verify(pharmacyPrescriptionRepository, never()).save(any(PharmacyPrescription.class));
        verify(medicineStockItemRepository, never()).saveAll(any());
        verify(stockConsumptionEventRepository, never()).saveAll(any());
    }

    private Pharmacy pharmacy(Long id, Long ownerUserId, String name, String licenseNumber) {
        return Pharmacy.builder()
            .id(id)
            .ownerUserId(ownerUserId)
            .name(name)
            .licenseNumber(licenseNumber)
            .build();
    }

    private PharmacyPrescription pendingWorkflow(Long id, Long sourcePrescriptionId, Long patientId, Pharmacy assignedPharmacy) {
        return PharmacyPrescription.builder()
            .id(id)
            .sourcePrescriptionId(sourcePrescriptionId)
            .assignedPharmacy(assignedPharmacy)
            .doctorId(10L)
            .patientId(patientId)
            .doctorNameSnapshot("Doctor")
            .patientNameSnapshot("Patient")
            .status(PrescriptionStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private PrescriptionLineResponseDTO line(Long id, String medicationName, String dosage, Integer quantity) {
        return PrescriptionLineResponseDTO.builder()
            .id(id)
            .medicationName(medicationName)
            .dosage(dosage)
            .quantity(quantity)
            .build();
    }
}
