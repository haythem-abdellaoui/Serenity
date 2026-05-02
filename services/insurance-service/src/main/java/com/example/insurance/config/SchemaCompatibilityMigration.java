package com.example.insurance.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaCompatibilityMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateEnumColumnsToVarchar() {
        alterColumnSafely("ALTER TABLE insurance_claims MODIFY COLUMN status VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE remboursements MODIFY COLUMN statut VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE insurance_notifications MODIFY COLUMN type VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE insurance_claim_transitions MODIFY COLUMN from_status VARCHAR(64) NULL");
        alterColumnSafely("ALTER TABLE insurance_claim_transitions MODIFY COLUMN to_status VARCHAR(64) NOT NULL");
        alterColumnSafely("ALTER TABLE insurance_claim_ocr_audit MODIFY COLUMN mismatch_details_json LONGTEXT NULL");
        alterColumnSafely("ALTER TABLE insurance_claim_ocr_audit MODIFY COLUMN extracted_text LONGTEXT NULL");
        alterColumnSafely("ALTER TABLE insurance_claim_ocr_audit MODIFY COLUMN mismatch_summary TEXT NULL");
    }

    private void alterColumnSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Schema compatibility migration applied: {}", sql);
        } catch (Exception ex) {
            // Ignore when table/column does not exist yet or is already compatible.
            log.debug("Schema compatibility migration skipped: {}. Reason: {}", sql, ex.getMessage());
        }
    }
}
