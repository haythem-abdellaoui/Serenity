package com.example.appointment.scheduler;

import com.example.appointment.service.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentNotificationService appointmentNotificationService;

    @Scheduled(fixedRate = 30_000)
    public void sendReminders() {
        appointmentNotificationService.runReminderTick();
    }
}
