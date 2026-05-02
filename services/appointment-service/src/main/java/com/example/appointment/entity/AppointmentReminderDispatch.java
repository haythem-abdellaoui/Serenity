package com.example.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_reminder_dispatches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"appointment_id", "reminder_kind"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentReminderDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_kind", nullable = false, length = 32)
    private AppointmentReminderKind reminderKind;
}
