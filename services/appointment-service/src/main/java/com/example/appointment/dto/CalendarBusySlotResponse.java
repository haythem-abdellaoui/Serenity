package com.example.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A blocking slot for calendar display: either the doctor or the patient is already booked then.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarBusySlotResponse {

    private LocalDate appointmentDate;
    private String timeSlot;
    /** DOCTOR = selected doctor is busy; PATIENT = selected/current patient is busy (other appts). */
    private String source;
}
