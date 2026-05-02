package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {
    private long totalPatients;
    private long activeRecords;
    private long activePrescriptions;
    private long severityLow;
    private long severityMedium;
    private long severityHigh;

    // AI Recommendation Statistics
    private long aiRecommendedCount;
    private long totalPrescriptionItems;
    private double aiAcceptanceRate;
    private List<Map<String, Object>> topAiMedicines;
}
