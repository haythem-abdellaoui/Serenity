package com.example.pharmacy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DerbelPrescriptionReadService {

    @Value("${app.derbel-db.url:jdbc:mysql://localhost:3306/medical_records_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}")
    private String derbelDbUrl;

    @Value("${app.derbel-db.username:root}")
    private String derbelDbUsername;

    @Value("${app.derbel-db.password:}")
    private String derbelDbPassword;

    public Map<Long, List<SourcePrescriptionItemRecord>> findItemsByPrescriptionIds(Collection<Long> prescriptionIds) {
        if (prescriptionIds == null || prescriptionIds.isEmpty()) {
            return Map.of();
        }

        List<Long> uniqueIds = prescriptionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = uniqueIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = """
            SELECT pi.id,
                   pi.prescription_id,
                   m.name AS medicine_name,
                   pi.dosage,
                   pi.frequency,
                   pi.quantity,
                   pi.start_date,
                   pi.end_date,
                   pi.instructions
            FROM prescription_items pi
            JOIN medicines m ON m.id = pi.medicine_id
            WHERE pi.prescription_id IN (%s)
            ORDER BY pi.prescription_id ASC, pi.id ASC
            """.formatted(placeholders);

        Map<Long, List<SourcePrescriptionItemRecord>> itemsByPrescription = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(derbelDbUrl, derbelDbUsername, derbelDbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < uniqueIds.size(); i++) {
                statement.setLong(i + 1, uniqueIds.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long prescriptionId = rs.getLong("prescription_id");
                    LocalDate startDate = rs.getDate("start_date") == null ? null : rs.getDate("start_date").toLocalDate();
                    LocalDate endDate = rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate();

                    SourcePrescriptionItemRecord item = new SourcePrescriptionItemRecord(
                        rs.getLong("id"),
                        prescriptionId,
                        rs.getString("medicine_name"),
                        rs.getString("dosage"),
                        rs.getString("frequency"),
                        rs.getInt("quantity"),
                        startDate,
                        endDate,
                        rs.getString("instructions")
                    );

                    itemsByPrescription.computeIfAbsent(prescriptionId, ignored -> new ArrayList<>()).add(item);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to read prescription items from derbel database", ex);
            return Map.of();
        }

        return itemsByPrescription;
    }

    public record SourcePrescriptionItemRecord(
        Long id,
        Long prescriptionId,
        String medicineName,
        String dosage,
        String frequency,
        Integer quantity,
        LocalDate startDate,
        LocalDate endDate,
        String instructions
    ) {}
}
