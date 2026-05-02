package com.example.healthcare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BanMaintenanceScheduler {

    private final BanMaintenanceService banMaintenanceService;

    // Default: every 30 minutes on the clock (:00 and :30)
    @Scheduled(cron = "${app.ban.cleanup-cron:0 0/30 * * * *}", zone = "${app.ban.cleanup-zone:UTC}")
    public void cleanupExpiredBans() {
        int cleaned = banMaintenanceService.unbanExpiredUsers();
        if (cleaned > 0) {
            log.info("Ban cleanup job completed. Automatically unbanned {} users.", cleaned);
        }
    }
}
