package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.AlternativePharmacyOptionDTO;
import com.example.pharmacy.dto.PerMedicineAlternativeDTO;
import com.example.pharmacy.dto.PrescriptionAlternativeResponseDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.entity.StockState;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionAlternativeService {

    private static final double FULL_MATCH_RADIUS_KM = 10.0;
    private static final double PARTIAL_FALLBACK_RADIUS_KM = 20.0;

    private final PharmacyRepository pharmacyRepository;
    private final MedicineStockItemRepository medicineStockItemRepository;

    public PrescriptionAlternativeResponseDTO buildAlternatives(
        Long prescriptionId,
        PrescriptionStatus status,
        List<PrescriptionLineQueryService.RequiredLine> requiredLines,
        Double latitude,
        Double longitude
    ) {
        List<PharmacyDistance> nearby10 = findNearbyPharmacies(latitude, longitude, FULL_MATCH_RADIUS_KM);
        Set<String> requiredMedicineNames = requiredLines.stream()
            .map(PrescriptionLineQueryService.RequiredLine::normalizedName)
            .collect(Collectors.toSet());

        Map<Long, Map<String, Integer>> stockMap10 = loadStockByPharmacy(nearby10, requiredMedicineNames);
        List<AlternativePharmacyOptionDTO> fullMatchPharmacies = nearby10.stream()
            .filter(candidate -> hasAllRequiredLines(stockMap10.getOrDefault(candidate.pharmacy().getId(), Map.of()), requiredLines))
            .map(candidate -> toAlternativeOption(candidate, null))
            .toList();

        if (!fullMatchPharmacies.isEmpty()) {
            return PrescriptionAlternativeResponseDTO.builder()
                .prescriptionId(prescriptionId)
                .status(status)
                .latitude(latitude)
                .longitude(longitude)
                .fullMatchRadiusKm(FULL_MATCH_RADIUS_KM)
                .partialRadiusKm(PARTIAL_FALLBACK_RADIUS_KM)
                .recommendedMode("FULL_MATCH")
                .message("Nearby pharmacies can fulfill all medicine lines")
                .fullMatchPharmacies(fullMatchPharmacies)
                .perMedicineAlternatives(List.of())
                .selectablePharmacies(fullMatchPharmacies)
                .build();
        }

        List<PharmacyDistance> nearby20 = findNearbyPharmacies(latitude, longitude, PARTIAL_FALLBACK_RADIUS_KM);
        Map<Long, Map<String, Integer>> stockMap20 = loadStockByPharmacy(nearby20, requiredMedicineNames);

        List<PerMedicineAlternativeDTO> perMedicineAlternatives = new ArrayList<>();
        Map<Long, AlternativePharmacyOptionDTO> selectable = new LinkedHashMap<>();

        for (PrescriptionLineQueryService.RequiredLine requiredLine : requiredLines) {
            List<AlternativePharmacyOptionDTO> options = nearby20.stream()
                .map(candidate -> {
                    Map<String, Integer> stock = stockMap20.getOrDefault(candidate.pharmacy().getId(), Map.of());
                    int availableQuantity = stock.getOrDefault(requiredLine.normalizedName(), 0);
                    if (availableQuantity < requiredLine.requiredQuantity()) {
                        return null;
                    }
                    return toAlternativeOption(candidate, availableQuantity);
                })
                .filter(java.util.Objects::nonNull)
                .toList();

            options.forEach(option -> selectable.put(option.getPharmacyId(), option));

            perMedicineAlternatives.add(PerMedicineAlternativeDTO.builder()
                .lineId(requiredLine.lineId())
                .medicationName(requiredLine.displayName())
                .requiredQuantity(requiredLine.requiredQuantity())
                .pharmacies(options)
                .build());
        }

        return PrescriptionAlternativeResponseDTO.builder()
            .prescriptionId(prescriptionId)
            .status(status)
            .latitude(latitude)
            .longitude(longitude)
            .fullMatchRadiusKm(FULL_MATCH_RADIUS_KM)
            .partialRadiusKm(PARTIAL_FALLBACK_RADIUS_KM)
            .recommendedMode(selectable.isEmpty() ? "NONE" : "PARTIAL_FALLBACK")
            .message(selectable.isEmpty()
                ? "No nearby pharmacies currently match the prescription requirements"
                : "No full-match pharmacy found within 10 km. Showing per-medicine alternatives within 20 km.")
            .fullMatchPharmacies(List.of())
            .perMedicineAlternatives(perMedicineAlternatives)
            .selectablePharmacies(new ArrayList<>(selectable.values()))
            .build();
    }

    public Set<Long> extractSelectablePharmacyIds(PrescriptionAlternativeResponseDTO alternatives) {
        return alternatives.getSelectablePharmacies().stream()
            .map(AlternativePharmacyOptionDTO::getPharmacyId)
            .collect(Collectors.toSet());
    }

    private List<PharmacyDistance> findNearbyPharmacies(Double latitude, Double longitude, double maxRadiusKm) {
        return pharmacyRepository.findAllWithCoordinates().stream()
            .map(pharmacy -> new PharmacyDistance(
                pharmacy,
                roundDistance(calculateDistanceKm(latitude, longitude, pharmacy.getLatitude(), pharmacy.getLongitude()))
            ))
            .filter(candidate -> candidate.distanceKm() <= maxRadiusKm)
            .sorted(Comparator.comparingDouble(PharmacyDistance::distanceKm))
            .limit(30)
            .toList();
    }

    private Map<Long, Map<String, Integer>> loadStockByPharmacy(
        List<PharmacyDistance> candidates,
        Set<String> medicineNames
    ) {
        if (candidates.isEmpty() || medicineNames.isEmpty()) {
            return Map.of();
        }

        Set<Long> pharmacyIds = candidates.stream().map(candidate -> candidate.pharmacy().getId()).collect(Collectors.toSet());
        List<MedicineStockItem> stockItems = medicineStockItemRepository.findActiveByPharmacyIdsAndMedicineNames(pharmacyIds, medicineNames);

        Map<Long, Map<String, Integer>> stockByPharmacy = new LinkedHashMap<>();
        for (MedicineStockItem stockItem : stockItems) {
            if (stockItem.getState() != null && stockItem.getState() != StockState.IN_STOCK) {
                continue;
            }
            Long pharmacyId = stockItem.getPharmacy().getId();
            String normalizedName = normalizeMedicineName(stockItem.getMedicineName());
            if (normalizedName == null) {
                continue;
            }

            stockByPharmacy.putIfAbsent(pharmacyId, new LinkedHashMap<>());
            Map<String, Integer> medicineStock = stockByPharmacy.get(pharmacyId);
            int safeQuantity = stockItem.getQuantity() == null ? 0 : Math.max(stockItem.getQuantity(), 0);
            medicineStock.put(normalizedName, medicineStock.getOrDefault(normalizedName, 0) + safeQuantity);
        }

        return stockByPharmacy;
    }

    private boolean hasAllRequiredLines(
        Map<String, Integer> pharmacyStock,
        List<PrescriptionLineQueryService.RequiredLine> requiredLines
    ) {
        for (PrescriptionLineQueryService.RequiredLine requiredLine : requiredLines) {
            int availableQuantity = pharmacyStock.getOrDefault(requiredLine.normalizedName(), 0);
            if (availableQuantity < requiredLine.requiredQuantity()) {
                return false;
            }
        }
        return true;
    }

    private AlternativePharmacyOptionDTO toAlternativeOption(PharmacyDistance candidate, Integer availableQuantity) {
        Pharmacy pharmacy = candidate.pharmacy();
        return AlternativePharmacyOptionDTO.builder()
            .pharmacyId(pharmacy.getId())
            .pharmacyName(pharmacy.getName())
            .addressLine(pharmacy.getAddressLine())
            .city(pharmacy.getCity())
            .governorate(pharmacy.getGovernorate())
            .latitude(pharmacy.getLatitude())
            .longitude(pharmacy.getLongitude())
            .distanceKm(candidate.distanceKm())
            .availableQuantity(availableQuantity)
            .build();
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

    private record PharmacyDistance(Pharmacy pharmacy, Double distanceKm) {}
}
