package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.*;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.entity.StockConsumptionEvent;
import com.example.pharmacy.entity.StockState;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.repository.StockConsumptionEventRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PrescriptionInsuranceDocumentPayload;
import com.example.pharmacy.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final Map<PrescriptionStatus, List<PrescriptionStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        PrescriptionStatus.PENDING, List.of(PrescriptionStatus.ACCEPTED, PrescriptionStatus.REJECTED),
        PrescriptionStatus.ACCEPTED, List.of(PrescriptionStatus.READY_FOR_PICKUP, PrescriptionStatus.REJECTED),
        PrescriptionStatus.READY_FOR_PICKUP, List.of(PrescriptionStatus.COLLECTED),
        PrescriptionStatus.REJECTED, List.of(),
        PrescriptionStatus.COLLECTED, List.of(),
        PrescriptionStatus.EXPIRED, List.of()
    );

    private final PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineStockItemRepository medicineStockItemRepository;
    private final CurrentUserService currentUserService;
    private final PrescriptionLineQueryService prescriptionLineQueryService;
    private final PrescriptionAlternativeService prescriptionAlternativeService;
    private final PrescriptionResponseMapper prescriptionResponseMapper;
    private final PrescriptionInsuranceDocumentStorageService prescriptionInsuranceDocumentStorageService;
    private final StockConsumptionEventRepository stockConsumptionEventRepository;

    @Override
    public List<PrescriptionResponseDTO> getMyInbox() {
        Long pharmacistId = currentUserService.getCurrentUserId();
        List<PharmacyPrescription> workflows = pharmacyPrescriptionRepository
            .findByAssignedPharmacyOwnerUserIdOrderByCreatedAtDesc(pharmacistId);

        return toResponses(workflows);
    }

    @Override
    public List<PrescriptionResponseDTO> getMyInsuranceMissingInbox() {
        Long pharmacistId = currentUserService.getCurrentUserId();
        List<PharmacyPrescription> workflows = pharmacyPrescriptionRepository
            .findByAssignedPharmacyOwnerUserIdAndStatusAndInsuranceDocumentPathIsNullOrderByReadyAtAsc(
                pharmacistId,
                PrescriptionStatus.READY_FOR_PICKUP
            );

        return toResponses(workflows);
    }

    @Override
    public List<PrescriptionResponseDTO> getMyPatientPrescriptions() {
        Long patientId = currentUserService.getCurrentUserId();
        List<PharmacyPrescription> workflows = pharmacyPrescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return toResponses(workflows);
    }

    @Override
    public PrescriptionResponseDTO getPrescription(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        boolean isOwnerPharmacist = isPharmacyOwner(workflow, currentUserId);
        boolean isPatient = workflow.getPatientId().equals(currentUserId);
        if (!(isOwnerPharmacist || isPatient)) {
            throw new AccessDeniedException("You cannot access this prescription");
        }

        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(workflow.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(workflow, lines);
    }

    @Override
    public PrescriptionAlternativeResponseDTO getPatientAlternatives(Long id, Double latitude, Double longitude) {
        requireCoordinates(latitude, longitude);
        Long patientId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = getOwnedPatientPrescription(id, patientId);

        if (workflow.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Alternative pharmacy recommendations are only available for pending prescriptions");
        }

        List<PrescriptionLineResponseDTO> liveLines =
            prescriptionLineQueryService.loadLinesForSource(workflow.getSourcePrescriptionId());
        List<PrescriptionLineQueryService.RequiredLine> requiredLines =
            prescriptionLineQueryService.resolveRequiredLines(liveLines);
        if (requiredLines.isEmpty()) {
            throw new IllegalStateException("Prescription does not contain medicine lines");
        }

        return prescriptionAlternativeService.buildAlternatives(
            workflow.getId(),
            workflow.getStatus(),
            requiredLines,
            latitude,
            longitude
        );
    }

    @Override
    public PrescriptionResponseDTO reassignPatientPrescriptionPharmacy(Long id, PrescriptionPharmacyReassignRequestDTO request) {
        requireCoordinates(request.getLatitude(), request.getLongitude());
        Long patientId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = getOwnedPatientPrescription(id, patientId);

        if (workflow.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Only pending prescriptions can be reassigned to another pharmacy");
        }

        PrescriptionAlternativeResponseDTO alternatives = getPatientAlternatives(id, request.getLatitude(), request.getLongitude());
        Set<Long> candidatePharmacyIds = prescriptionAlternativeService.extractSelectablePharmacyIds(alternatives);

        if (!candidatePharmacyIds.contains(request.getPharmacyId())) {
            throw new IllegalArgumentException("Selected pharmacy is not part of the recommendation results");
        }

        Pharmacy selectedPharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId()));

        workflow.setAssignedPharmacy(selectedPharmacy);
        PharmacyPrescription saved = pharmacyPrescriptionRepository.save(workflow);
        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(saved.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(saved, lines);
    }

    @Override
    public PrescriptionResponseDTO updatePrescriptionStatus(Long id, PrescriptionStatusUpdateRequestDTO request) {
        Long pharmacistId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        ensureAssignedToPharmacyOwner(workflow, pharmacistId);

        PrescriptionStatus nextStatus = request.getStatus();
        if (nextStatus == PrescriptionStatus.PENDING || nextStatus == PrescriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Invalid status update for pharmacist workflow");
        }

        if (nextStatus == workflow.getStatus()) {
            throw new IllegalArgumentException("Prescription is already in status " + workflow.getStatus());
        }

        if (!isAllowedTransition(workflow.getStatus(), nextStatus)) {
            throw new IllegalStateException(
                "Invalid status transition from " + workflow.getStatus() + " to " + nextStatus
            );
        }

        String normalizedRejectionReason = normalizeRejectionReason(request.getRejectionReason());
        if (nextStatus == PrescriptionStatus.REJECTED && normalizedRejectionReason == null) {
            throw new IllegalArgumentException("Rejection reason is required when rejecting a prescription");
        }

        workflow.setStatus(nextStatus);
        workflow.setRejectionReason(nextStatus == PrescriptionStatus.REJECTED ? normalizedRejectionReason : null);

        if (nextStatus == PrescriptionStatus.READY_FOR_PICKUP) {
            decrementStockForReadyForPickup(workflow);
            workflow.setReadyAt(LocalDateTime.now());
        } else if (nextStatus == PrescriptionStatus.ACCEPTED || nextStatus == PrescriptionStatus.REJECTED) {
            workflow.setReadyAt(null);
        }

        PharmacyPrescription saved = pharmacyPrescriptionRepository.save(workflow);
        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(saved.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(saved, lines);
    }

    @Override
    public PrescriptionResponseDTO uploadInsuranceDocument(Long id, MultipartFile file) {
        Long pharmacistId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        ensureAssignedToPharmacyOwner(workflow, pharmacistId);
        if (workflow.getStatus() != PrescriptionStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Insurance document upload is allowed only when prescription is READY_FOR_PICKUP");
        }

        String storedPath = prescriptionInsuranceDocumentStorageService.storeDocument(
            workflow.getId(),
            file,
            workflow.getInsuranceDocumentPath()
        );

        workflow.setInsuranceDocumentPath(storedPath);
        workflow.setInsuranceDocumentUploadedAt(LocalDateTime.now());

        PharmacyPrescription saved = pharmacyPrescriptionRepository.save(workflow);
        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(saved.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(saved, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionInsuranceDocumentPayload getInsuranceDocument(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        boolean isOwnerPharmacist = isPharmacyOwner(workflow, currentUserId);
        boolean isPatient = workflow.getPatientId().equals(currentUserId);
        if (!(isOwnerPharmacist || isPatient || isAdmin())) {
            throw new AccessDeniedException("You cannot access this prescription insurance document");
        }

        String storedPath = workflow.getInsuranceDocumentPath();
        if (storedPath == null || storedPath.isBlank()) {
            throw new ResourceNotFoundException("InsuranceDocument", "prescriptionId", id);
        }

        return new PrescriptionInsuranceDocumentPayload(
            prescriptionInsuranceDocumentStorageService.loadDocument(storedPath),
            prescriptionInsuranceDocumentStorageService.resolveContentType(storedPath),
            prescriptionInsuranceDocumentStorageService.resolveFileName(storedPath, workflow.getId())
        );
    }

    private List<PrescriptionResponseDTO> toResponses(List<PharmacyPrescription> workflows) {
        if (workflows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PrescriptionLineResponseDTO>> linesBySourceId = prescriptionLineQueryService.loadLinesBySourceIds(
            workflows.stream().map(PharmacyPrescription::getSourcePrescriptionId).toList()
        );

        return workflows.stream()
            .map(workflow -> prescriptionResponseMapper.toResponse(
                workflow,
                linesBySourceId.getOrDefault(workflow.getSourcePrescriptionId(), List.of())
            ))
            .toList();
    }

    private boolean isAllowedTransition(PrescriptionStatus currentStatus, PrescriptionStatus nextStatus) {
        return ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, List.of()).contains(nextStatus);
    }

    private String normalizeRejectionReason(String rejectionReason) {
        if (rejectionReason == null) {
            return null;
        }
        String normalized = rejectionReason.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void ensureAssignedToPharmacyOwner(PharmacyPrescription workflow, Long pharmacistId) {
        if (workflow.getAssignedPharmacy() == null) {
            throw new IllegalStateException("This prescription is not yet assigned to a pharmacy");
        }

        if (!isPharmacyOwner(workflow, pharmacistId)) {
            throw new AccessDeniedException("You can only update prescriptions assigned to your pharmacy");
        }
    }

    private boolean isPharmacyOwner(PharmacyPrescription workflow, Long userId) {
        return workflow.getAssignedPharmacy() != null && workflow.getAssignedPharmacy().getOwnerUserId().equals(userId);
    }

    private PharmacyPrescription getOwnedPatientPrescription(Long prescriptionId, Long patientId) {
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", prescriptionId));

        if (!workflow.getPatientId().equals(patientId)) {
            throw new AccessDeniedException("You can only manage your own prescriptions");
        }

        return workflow;
    }

    private void requireCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
    }

    private void decrementStockForReadyForPickup(PharmacyPrescription workflow) {
        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(workflow.getSourcePrescriptionId());
        List<PrescriptionLineQueryService.RequiredLine> requiredLines =
            prescriptionLineQueryService.resolveRequiredLines(lines);

        if (requiredLines.isEmpty()) {
            throw new IllegalStateException("Cannot mark prescription as ready: no medicine lines were found");
        }

        Map<String, Integer> requiredByMedicine = new LinkedHashMap<>();
        Map<String, String> displayNameByNormalizedMedicine = new LinkedHashMap<>();
        for (PrescriptionLineQueryService.RequiredLine line : requiredLines) {
            String normalizedName = line.normalizedName();
            if (normalizedName == null) {
                continue;
            }
            requiredByMedicine.merge(normalizedName, line.requiredQuantity(), Integer::sum);
            displayNameByNormalizedMedicine.putIfAbsent(normalizedName, line.displayName());
        }

        Set<String> normalizedMedicineNames = requiredByMedicine.keySet();
        List<MedicineStockItem> activeStockItems = medicineStockItemRepository.findActiveByPharmacyIdsAndMedicineNames(
            Set.of(workflow.getAssignedPharmacy().getId()),
            normalizedMedicineNames
        );

        Map<String, List<MedicineStockItem>> stockByMedicine = activeStockItems.stream()
            .collect(Collectors.groupingBy(
                item -> normalizeMedicineName(item.getMedicineName()),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<MedicineStockItem> changedItems = new ArrayList<>();
        Map<String, Integer> consumedByMedicine = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> requiredEntry : requiredByMedicine.entrySet()) {
            String normalizedMedicine = requiredEntry.getKey();
            int requiredQuantity = requiredEntry.getValue();
            String displayName = displayNameByNormalizedMedicine.getOrDefault(normalizedMedicine, normalizedMedicine);

            List<MedicineStockItem> candidates = stockByMedicine.getOrDefault(normalizedMedicine, List.of()).stream()
                .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                .sorted(Comparator.comparing(MedicineStockItem::getQuantity).reversed())
                .toList();

            int remaining = requiredQuantity;
            for (MedicineStockItem item : candidates) {
                if (remaining <= 0) {
                    break;
                }
                int currentQuantity = item.getQuantity() == null ? 0 : item.getQuantity();
                int take = Math.min(currentQuantity, remaining);
                if (take <= 0) {
                    continue;
                }

                int nextQuantity = currentQuantity - take;
                item.setQuantity(nextQuantity);
                item.setState(nextQuantity > 0 ? StockState.IN_STOCK : StockState.OUT_OF_STOCK);
                changedItems.add(item);
                consumedByMedicine.merge(normalizedMedicine, take, Integer::sum);
                remaining -= take;
            }

            if (remaining > 0) {
                int availableQuantity = requiredQuantity - remaining;
                throw new IllegalStateException(
                    "Cannot mark prescription as ready. Insufficient stock for " + displayName
                        + " (required: " + requiredQuantity + ", available: " + availableQuantity + ")"
                );
            }
        }

        if (!changedItems.isEmpty()) {
            medicineStockItemRepository.saveAll(changedItems);
            LocalDateTime eventTime = LocalDateTime.now();
            List<StockConsumptionEvent> events = consumedByMedicine.entrySet().stream()
                .map(entry -> StockConsumptionEvent.builder()
                    .pharmacy(workflow.getAssignedPharmacy())
                    .medicineName(displayNameByNormalizedMedicine.getOrDefault(entry.getKey(), entry.getKey()))
                    .consumedQty(entry.getValue())
                    .eventAt(eventTime)
                    .sourcePrescriptionId(workflow.getSourcePrescriptionId())
                    .build())
                .toList();
            stockConsumptionEventRepository.saveAll(events);
        }
    }

    private String normalizeMedicineName(String medicineName) {
        if (medicineName == null) {
            return null;
        }
        String normalized = medicineName.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isAdmin() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return false;
        }

        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    }
}
