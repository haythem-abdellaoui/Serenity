package com.example.appointment.dto;

import com.example.appointment.entity.AppointmentStatus;
import com.example.appointment.entity.AppointmentType;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AppointmentResponse {

    private Long id;
    private Long patientUserId;
    private Long doctorUserId;
    private String patientFirstName;
    private String patientLastName;
    private String doctorFirstName;
    private String doctorLastName;
    private LocalDate appointmentDate;
    private String timeSlot;
    private AppointmentStatus status;
    private AppointmentType type;
    private String notes;
    private Instant createdAt;
    private TeleconsultationResponse teleconsultation;
}
