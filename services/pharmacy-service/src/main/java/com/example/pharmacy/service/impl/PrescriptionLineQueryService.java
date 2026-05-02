package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PrescriptionLineResponseDTO;
import com.example.pharmacy.service.DerbelPrescriptionReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrescriptionLineQueryService {

    private final DerbelPrescriptionReadService derbelPrescriptionReadService;

    public List<PrescriptionLineResponseDTO> loadLinesForSource(Long sourcePrescriptionId) {
        Map<Long, List<PrescriptionLineResponseDTO>> bySource = loadLinesBySourceIds(List.of(sourcePrescriptionId));
        return bySource.getOrDefault(sourcePrescriptionId, List.of());
    }

    public Map<Long, List<PrescriptionLineResponseDTO>> loadLinesBySourceIds(Collection<Long> sourcePrescriptionIds) {
        Map<Long, List<DerbelPrescriptionReadService.SourcePrescriptionItemRecord>> sourceItemsByPrescription =
            derbelPrescriptionReadService.findItemsByPrescriptionIds(sourcePrescriptionIds);

        Map<Long, List<PrescriptionLineResponseDTO>> linesBySource = new LinkedHashMap<>();
        for (Map.Entry<Long, List<DerbelPrescriptionReadService.SourcePrescriptionItemRecord>> entry : sourceItemsByPrescription.entrySet()) {
            List<PrescriptionLineResponseDTO> lines = entry.getValue().stream()
                .map(this::toLineResponse)
                .toList();
            linesBySource.put(entry.getKey(), lines);
        }

        return linesBySource;
    }

    public List<RequiredLine> resolveRequiredLines(List<PrescriptionLineResponseDTO> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        List<RequiredLine> requiredLines = new ArrayList<>();
        for (PrescriptionLineResponseDTO line : lines) {
            String displayName = safeTrim(line.getMedicationName());
            String normalizedName = normalizeMedicineName(displayName);
            if (normalizedName == null) {
                continue;
            }

            int requiredQuantity = line.getQuantity() == null || line.getQuantity() <= 0 ? 1 : line.getQuantity();
            requiredLines.add(new RequiredLine(line.getId(), displayName, normalizedName, requiredQuantity));
        }
        return requiredLines;
    }

    private PrescriptionLineResponseDTO toLineResponse(DerbelPrescriptionReadService.SourcePrescriptionItemRecord sourceItem) {
        return PrescriptionLineResponseDTO.builder()
            .id(sourceItem.id())
            .medicationName(sourceItem.medicineName())
            .dosage(sourceItem.dosage())
            .frequency(sourceItem.frequency())
            .quantity(sourceItem.quantity())
            .startDate(sourceItem.startDate() != null ? sourceItem.startDate().toString() : null)
            .endDate(sourceItem.endDate() != null ? sourceItem.endDate().toString() : null)
            .instructions(sourceItem.instructions())
            .build();
    }

    private String normalizeMedicineName(String medicineName) {
        String value = safeTrim(medicineName);
        return value == null ? null : value.toLowerCase();
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record RequiredLine(Long lineId, String displayName, String normalizedName, Integer requiredQuantity) {}
}
