package com.example.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentNotificationResponse {

    private Long id;
    private Long userId;
    private Long appointmentId;
    private String type;
    private String title;
    private String message;
    private boolean isRead;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
