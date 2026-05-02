package tn.esprit.arctic.derbelmicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.DashboardDTO;
import tn.esprit.arctic.derbelmicroservice.entity.enums.Severity;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.repository.PrescriptionRepository;
import tn.esprit.arctic.derbelmicroservice.repository.PrescriptionItemRepository;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixDb() {
        try {
            String query = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = 'medical_records' AND COLUMN_NAME = 'patient_id' AND REFERENCED_TABLE_NAME = 'patients' AND TABLE_SCHEMA = 'medical_records_db' LIMIT 1";
            String constraintName = jdbcTemplate.queryForObject(query, String.class);
            if (constraintName != null) {
                jdbcTemplate.execute("ALTER TABLE medical_records DROP FOREIGN KEY " + constraintName);
                System.out.println("DROPPED FOREIGN KEY: " + constraintName);
            }
        } catch (Exception e) {
            System.out.println("Fix DB skipped: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<DashboardDTO>> getStats() {
        DerbelAuth.requireDoctorOrAdmin();
        Long userId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        DashboardDTO dto;
        if (isAdmin) {
            dto = DashboardDTO.builder()
                    .totalPatients(0L) // Deferred to user-service / frontend
                    .activeRecords(medicalRecordRepository.countByStatusIgnoreCase("ACTIVE"))
                    .activePrescriptions(prescriptionRepository.countByStatusIgnoreCase("ACTIVE"))
                    .severityLow(medicalRecordRepository.countBySeverity(Severity.LOW))
                    .severityMedium(medicalRecordRepository.countBySeverity(Severity.MEDIUM))
                    .severityHigh(medicalRecordRepository.countBySeverity(Severity.HIGH))
                    .build();
        } else {
            dto = DashboardDTO.builder()
                    .totalPatients(0L) // Deferred to user-service / frontend
                    .activeRecords(medicalRecordRepository.countByDoctorIdAndStatusIgnoreCase(userId, "ACTIVE"))
                    .activePrescriptions(prescriptionRepository.countByDoctorIdAndStatusIgnoreCase(userId, "ACTIVE"))
                    .severityLow(medicalRecordRepository.countByDoctorIdAndSeverity(userId, Severity.LOW))
                    .severityMedium(medicalRecordRepository.countByDoctorIdAndSeverity(userId, Severity.MEDIUM))
                    .severityHigh(medicalRecordRepository.countByDoctorIdAndSeverity(userId, Severity.HIGH))
                    .build();
        }

        // AI Recommendation Statistics (global, shared for all roles)
        long aiCount = prescriptionItemRepository.countByIsAiRecommended(true);
        long totalItems = prescriptionItemRepository.count();
        double rate = totalItems > 0 ? Math.round((double) aiCount / totalItems * 10000.0) / 100.0 : 0.0;

        List<Object[]> topRaw = prescriptionItemRepository.findTopAiRecommendedMedicines();
        List<Map<String, Object>> topAiMedicines = topRaw.stream()
                .limit(5)
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", row[0]);
                    m.put("count", row[1]);
                    return m;
                })
                .collect(Collectors.toList());

        dto.setAiRecommendedCount(aiCount);
        dto.setTotalPrescriptionItems(totalItems);
        dto.setAiAcceptanceRate(rate);
        dto.setTopAiMedicines(topAiMedicines);

        return ResponseEntity.ok(ApiResponseDTO.<DashboardDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Dashboard statistics")
                .data(dto)
                .build());
    }
}
