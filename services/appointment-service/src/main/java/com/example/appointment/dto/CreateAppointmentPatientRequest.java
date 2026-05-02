package com.example.appointment.dto;

import com.example.appointment.entity.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentPatientRequest {

    @NotNull
    @Positive
    private Long doctorUserId;

    @NotNull
    private LocalDate appointmentDate;

    @NotBlank
    @Size(min = 3, max = 8)
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "timeSlot must match HH:mm format (24h)"
    )
    private String timeSlot;

    @NotNull
    private AppointmentType type;

    @Size(max = 2000)
    private String notes;
}
