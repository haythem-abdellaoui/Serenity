package tn.esprit.arctic.derbelmicroservice.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionItemRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionItemResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Medicine;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;
import tn.esprit.arctic.derbelmicroservice.entity.PrescriptionItem;
import tn.esprit.arctic.derbelmicroservice.repository.MedicineRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PrescriptionMapper {

    private final MedicineRepository medicineRepository;
    private final MedicineMapper medicineMapper;

    public PrescriptionResponseDTO toResponseDTO(Prescription p) {
        return PrescriptionResponseDTO.builder()
                .id(p.getId())
                .medicalRecordId(p.getMedicalRecord().getId())
                .patientId(p.getPatientId())
                .doctorId(p.getDoctorId())
                .status(p.getStatus())
                .items(toItemResponses(p.getItems()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .imageUrl(p.getImageUrl())
                .build();
    }

    public Prescription toEntity(PrescriptionRequestDTO dto, MedicalRecord medicalRecord) {
        Prescription prescription = Prescription.builder()
                .medicalRecord(medicalRecord)
                .patientId(dto.getPatientId())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        List<PrescriptionItem> items = toItemEntities(dto.getItems(), prescription);
        prescription.setItems(items);
        return prescription;
    }

    public void updateEntity(PrescriptionRequestDTO dto, Prescription prescription) {
        prescription.setStatus(dto.getStatus() != null ? dto.getStatus() : prescription.getStatus());
        prescription.getItems().clear();
        prescription.getItems().addAll(toItemEntities(dto.getItems(), prescription));
    }

    private List<PrescriptionItem> toItemEntities(List<PrescriptionItemRequestDTO> dtos, Prescription prescription) {
        if (dtos == null || dtos.isEmpty()) {
            throw new IllegalArgumentException("Au moins un item est obligatoire");
        }
        return dtos.stream().map(dto -> {
            Medicine medicine = medicineRepository.findById(dto.getMedicineId())
                    .orElseThrow(() -> new IllegalArgumentException("Médicament introuvable: ID " + dto.getMedicineId()));
            return PrescriptionItem.builder()
                    .prescription(prescription)
                    .medicine(medicine)
                    .dosage(dto.getDosage().trim())
                    .frequency(dto.getFrequency().trim())
                    .quantity(dto.getQuantity())
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .instructions(dto.getInstructions() != null ? dto.getInstructions().trim() : null)
                    .isAiRecommended(dto.getIsAiRecommended() != null ? dto.getIsAiRecommended() : false)
                    .build();
        }).toList();
    }

    private List<PrescriptionItemResponseDTO> toItemResponses(List<PrescriptionItem> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toItemResponse).toList();
    }

    private PrescriptionItemResponseDTO toItemResponse(PrescriptionItem item) {
        MedicineResponseDTO medicineDTO = medicineMapper.toResponseDTO(item.getMedicine());
        return PrescriptionItemResponseDTO.builder()
                .id(item.getId())
                .medicine(medicineDTO)
                .dosage(item.getDosage())
                .frequency(item.getFrequency())
                .quantity(item.getQuantity())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .instructions(item.getInstructions())
                .isAiRecommended(item.getIsAiRecommended())
                .build();
    }
}
