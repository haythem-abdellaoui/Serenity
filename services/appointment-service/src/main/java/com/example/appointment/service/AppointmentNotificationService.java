package com.example.appointment.service;

import com.example.appointment.dto.AppointmentNotificationResponse;
import com.example.appointment.dto.AppointmentUnreadCountResponse;
import com.example.appointment.entity.Appointment;

import java.util.List;

public interface AppointmentNotificationService {

    /** In-app + email to doctor when a patient requests a new appointment. */
    void notifyPatientRequested(Appointment appt);

    void notifyScheduledByDoctor(Appointment appt);

    void notifyConfirmed(Appointment appt);

    void notifyRescheduled(Appointment appt);

    List<AppointmentNotificationResponse> getMyNotifications(Long userId);

    AppointmentUnreadCountResponse countUnread(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    /** Called every minute to send 1d / 1h / 5m reminders. */
    void runReminderTick();
}
