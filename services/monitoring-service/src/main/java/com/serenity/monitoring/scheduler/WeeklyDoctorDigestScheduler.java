package com.serenity.monitoring.scheduler;

import com.serenity.monitoring.service.WeeklyDoctorDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyDoctorDigestScheduler {

    private final WeeklyDoctorDigestService weeklyDoctorDigestService;

    @Scheduled(cron = "${app.digest.cron:0 0 8 ? * MON}", zone = "${app.digest.timezone:Africa/Tunis}")
    public void runWeeklyDoctorDigest() {
        log.info("Starting weekly doctor digest scheduler");
        weeklyDoctorDigestService.generateWeeklyDigests();
    }
}

