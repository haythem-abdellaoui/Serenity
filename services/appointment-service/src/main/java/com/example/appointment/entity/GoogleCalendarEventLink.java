package com.example.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "google_calendar_event_links", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "appointment_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleCalendarEventLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(nullable = false, length = 256)
    private String googleEventId;
}
