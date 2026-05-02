package com.example.appointment.service.impl;

import com.example.appointment.dto.AppointmentNotificationResponse;
import com.example.appointment.dto.AppointmentUnreadCountResponse;
import com.example.appointment.entity.*;
import com.example.appointment.integration.AppointmentMailService;
import com.example.appointment.integration.UserEmailClient;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.AppointmentReminderDispatchRepository;
import com.example.appointment.repository.AppointmentUserNotificationRepository;
import com.example.appointment.service.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentNotificationServiceImpl implements AppointmentNotificationService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentUserNotificationRepository notificationRepository;
    private final AppointmentReminderDispatchRepository dispatchRepository;
    private final UserEmailClient userEmailClient;
    private final AppointmentMailService appointmentMailService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPatientRequested(Appointment appt) {
        String when = formatWhen(appt);
        save(appt.getDoctorUserId(), appt.getId(), AppointmentNotificationType.PATIENT_REQUESTED,
                "New appointment request",
                "A patient requested a visit on " + when + ".");
        Map<Long, String> emails = userEmailClient.fetchEmailsByUserIds(List.of(appt.getDoctorUserId()));
        String doctorEmail = emails.get(appt.getDoctorUserId());
        if (doctorEmail == null) {
            log.warn(
                    "No doctor email resolved for userId={} — skipping new-booking email (check user profile email and INTERNAL_API_KEY).",
                    appt.getDoctorUserId());
        } else {
            appointmentMailService.sendReminderEmail(
                    doctorEmail,
                    "Serenity: new appointment request",
                    "A patient requested an appointment for " + when
                            + " (reference #" + appt.getId() + "). Log in to Serenity to confirm or contact the patient.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyScheduledByDoctor(Appointment appt) {
        String when = formatWhen(appt);
        save(appt.getPatientUserId(), appt.getId(), AppointmentNotificationType.SCHEDULED_BY_DOCTOR,
                "New appointment scheduled",
                "A doctor scheduled an appointment for you on " + when + ".");
        Map<Long, String> emails = userEmailClient.fetchEmailsByUserIds(List.of(appt.getPatientUserId()));
        String patientEmail = emails.get(appt.getPatientUserId());
        if (patientEmail == null) {
            log.warn("No patient email resolved for userId={} — skipping scheduled-by-doctor email.", appt.getPatientUserId());
        } else {
            appointmentMailService.sendReminderEmail(
                    patientEmail,
                    "Serenity: appointment scheduled for you",
                    "A doctor scheduled a visit for " + when + " (reference #" + appt.getId() + "). Log in to Serenity for details.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyConfirmed(Appointment appt) {
        String when = formatWhen(appt);
        save(appt.getPatientUserId(), appt.getId(), AppointmentNotificationType.CONFIRMED,
                "Appointment confirmed",
                "Your appointment on " + when + " is confirmed.");
        save(appt.getDoctorUserId(), appt.getId(), AppointmentNotificationType.CONFIRMED,
                "Appointment confirmed",
                "You confirmed appointment #" + appt.getId() + " for " + when + ".");
        Map<Long, String> emails = userEmailClient.fetchEmailsByUserIds(List.of(appt.getPatientUserId()));
        String patientEmail = emails.get(appt.getPatientUserId());
        if (patientEmail == null) {
            log.warn("No patient email resolved for userId={} — skipping confirmation email.", appt.getPatientUserId());
        } else {
            appointmentMailService.sendReminderEmail(
                    patientEmail,
                    "Serenity: your appointment is confirmed",
                    "Your visit on " + when + " is confirmed by your doctor. You can review details in Serenity.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyRescheduled(Appointment appt) {
        String when = formatWhen(appt);
        save(appt.getPatientUserId(), appt.getId(), AppointmentNotificationType.RESCHEDULED,
                "Appointment rescheduled",
                "Your appointment was moved to " + when + ".");
        save(appt.getDoctorUserId(), appt.getId(), AppointmentNotificationType.RESCHEDULED,
                "Appointment rescheduled",
                "Appointment #" + appt.getId() + " was rescheduled to " + when + ".");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentNotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentUnreadCountResponse countUnread(Long userId) {
        return new AppointmentUnreadCountResponse(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        AppointmentUserNotification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<AppointmentUserNotification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (AppointmentUserNotification n : list) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runReminderTick() {
        Instant now = Instant.now();
        LocalDate min = LocalDate.now().minusDays(1);
        List<Appointment> list = appointmentRepository.findByStatusFromDate(
                AppointmentStatus.CONFIRMED, min);
        for (Appointment a : list) {
            try {
                Instant start = appointmentStartInstant(a.getAppointmentDate(), a.getTimeSlot());
                if (!start.isAfter(now)) {
                    continue;
                }
                dispatchReminderIfDue(a, AppointmentReminderKind.ONE_DAY, start.minus(1, ChronoUnit.DAYS), now, start, 180);
                dispatchReminderIfDue(a, AppointmentReminderKind.FIVE_HOURS, start.minus(5, ChronoUnit.HOURS), now, start, 30);
                dispatchReminderIfDue(a, AppointmentReminderKind.ONE_HOUR, start.minus(1, ChronoUnit.HOURS), now, start, 15);
                dispatchReminderIfDue(a, AppointmentReminderKind.FIVE_MINUTES, start.minus(5, ChronoUnit.MINUTES), now, start, 10);
            } catch (Exception e) {
                log.warn("Reminder tick skipped for appointment id={}: {}", a.getId(), e.getMessage());
            }
        }
    }

    private void save(Long userId, Long appointmentId, AppointmentNotificationType type, String title, String message) {
        notificationRepository.save(AppointmentUserNotification.builder()
                .userId(userId)
                .appointmentId(appointmentId)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build());
    }

    private AppointmentNotificationResponse toDto(AppointmentUserNotification n) {
        return AppointmentNotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .appointmentId(n.getAppointmentId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(Boolean.TRUE.equals(n.getIsRead()))
                .createdAt(n.getCreatedAt())
                .build();
    }

    private static String formatWhen(Appointment appt) {
        return appt.getAppointmentDate() + " at " + appt.getTimeSlot();
    }

    /**
     * Send reminder once when {@code now} enters the window starting at {@code trigger}.
     * Uses a multi-minute window so the 1-minute scheduler does not miss the slot.
     */
    /**
     * @param windowMinutes how long after {@code trigger} we still send the reminder (covers scheduler tick gaps)
     */
    private void dispatchReminderIfDue(
            Appointment appt,
            AppointmentReminderKind kind,
            Instant trigger,
            Instant now,
            Instant appointmentStart,
            long windowMinutes) {
        if (dispatchRepository.existsByAppointmentIdAndReminderKind(appt.getId(), kind)) {
            return;
        }
        Instant windowEnd = trigger.plus(windowMinutes, ChronoUnit.MINUTES);
        Instant effectiveEnd = windowEnd.isBefore(appointmentStart) ? windowEnd : appointmentStart;
        if (now.isBefore(trigger) || !now.isBefore(effectiveEnd)) {
            return;
        }
        dispatchRepository.save(AppointmentReminderDispatch.builder()
                .appointmentId(appt.getId())
                .reminderKind(kind)
                .build());

        AppointmentNotificationType nType = switch (kind) {
            case ONE_DAY -> AppointmentNotificationType.REMINDER_1_DAY;
            case FIVE_HOURS -> AppointmentNotificationType.REMINDER_5_HOURS;
            case ONE_HOUR -> AppointmentNotificationType.REMINDER_1_HOUR;
            case FIVE_MINUTES -> AppointmentNotificationType.REMINDER_5_MIN;
        };

        String when = formatWhen(appt);
        String title = switch (kind) {
            case ONE_DAY -> "Appointment in 24 hours";
            case FIVE_HOURS -> "Appointment in 5 hours";
            case ONE_HOUR -> "Appointment in 1 hour";
            case FIVE_MINUTES -> "Appointment in 5 minutes";
        };
        String message = switch (kind) {
            case ONE_DAY -> "Reminder: you have an appointment in about 24 hours on " + when + ".";
            case FIVE_HOURS -> "Reminder: your appointment is in 5 hours (" + when + ").";
            case ONE_HOUR -> "Reminder: your appointment is in 1 hour (" + when + ").";
            case FIVE_MINUTES -> "Reminder: your appointment starts in 5 minutes (" + when + ").";
        };

        // Patient-only for scheduled reminders (in-app + email)
        save(appt.getPatientUserId(), appt.getId(), nType, title, message);

        Map<Long, String> emails = userEmailClient.fetchEmailsByUserIds(List.of(appt.getPatientUserId()));
        String patientEmail = emails.get(appt.getPatientUserId());
        if (patientEmail != null) {
            appointmentMailService.sendReminderEmail(patientEmail, title, message);
        }
    }

    private static Instant appointmentStartInstant(LocalDate date, String timeSlot) {
        String[] p = timeSlot.trim().split(":");
        if (p.length < 2) {
            throw new IllegalArgumentException("Invalid timeSlot: " + timeSlot);
        }
        int h = Integer.parseInt(p[0].trim());
        int m = Integer.parseInt(p[1].trim());
        LocalDateTime ldt = date.atTime(h, m);
        return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }
}
