package com.example.appointment.entity;

public enum AppointmentNotificationType {
    /** Patient created a pending booking (notifies doctor). */
    PATIENT_REQUESTED,
    SCHEDULED_BY_DOCTOR,
    CONFIRMED,
    RESCHEDULED,
    REMINDER_1_DAY,
    REMINDER_5_HOURS,
    REMINDER_1_HOUR,
    REMINDER_5_MIN
}
