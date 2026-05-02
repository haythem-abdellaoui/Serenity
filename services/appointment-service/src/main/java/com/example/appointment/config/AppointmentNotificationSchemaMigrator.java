package com.example.appointment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * Older databases may have {@code appointment_user_notifications.type} as a short VARCHAR or
 * a MySQL ENUM missing newer values (e.g. PATIENT_REQUESTED), which causes "Data truncated for column 'type'"
 * on patient booking. Widen to VARCHAR(64) on startup (safe to re-run).
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class AppointmentNotificationSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMysql()) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE appointment_user_notifications MODIFY COLUMN type VARCHAR(64) NOT NULL");
            log.info("Ensured appointment_user_notifications.type is VARCHAR(64)");
        } catch (Exception e) {
            log.warn("Could not alter appointment_user_notifications.type (table missing or already OK): {}",
                    e.getMessage());
        }
    }

    private boolean isMysql() {
        try {
            javax.sql.DataSource ds = jdbcTemplate.getDataSource();
            if (ds == null) {
                return false;
            }
            try (Connection c = ds.getConnection()) {
                String product = c.getMetaData().getDatabaseProductName();
                if (product == null) {
                    return false;
                }
                String p = product.toLowerCase();
                return p.contains("mysql") || p.contains("mariadb");
            }
        } catch (Exception e) {
            return false;
        }
    }
}
