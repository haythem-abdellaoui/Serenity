package tn.esprit.arctic.derbelmicroservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicineRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.Medicine;

@Component
public class MedicineMapper {

    public MedicineResponseDTO toResponseDTO(Medicine entity) {
        return MedicineResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .sideEffects(entity.getSideEffects())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Medicine toEntity(MedicineRequestDTO dto) {
        return Medicine.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .sideEffects(dto.getSideEffects() != null ? dto.getSideEffects().trim() : null)
                .build();
    }

    public void updateEntity(MedicineRequestDTO dto, Medicine entity) {
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        entity.setSideEffects(dto.getSideEffects() != null ? dto.getSideEffects().trim() : null);
    }
}
