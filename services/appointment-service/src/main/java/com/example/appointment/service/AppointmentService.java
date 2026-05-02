package com.example.appointment.service;

import com.example.appointment.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentResponse patientRequest(Long patientUserId, CreateAppointmentPatientRequest request);

    AppointmentResponse doctorSchedule(Long doctorUserId, CreateAppointmentDoctorRequest request);

    AppointmentResponse confirm(Long appointmentId, Long doctorUserId);

    AppointmentResponse cancel(Long appointmentId, Long currentUserId);

    AppointmentResponse complete(Long appointmentId, Long doctorUserId);

    List<AppointmentResponse> listMine(Long userId);

    List<AppointmentResponse> listAll();

    AppointmentResponse getById(Long id, Long userId, boolean admin);

    TeleconsultationResponse getTeleconsultation(Long appointmentId, Long userId, boolean admin);

    TeleconsultationResponse startTeleconsultation(Long appointmentId, Long userId);

    /**
     * Busy slots for calendar UI: patient mode merges doctor + current patient; doctor mode merges doctor + optional patient.
     */
    List<CalendarBusySlotResponse> calendarHintsForPatient(
            Long patientUserId, Long doctorUserId, LocalDate from, LocalDate to, Long excludeAppointmentId);

    List<CalendarBusySlotResponse> calendarHintsForDoctor(
            Long doctorUserId, Long patientUserIdOrNull, LocalDate from, LocalDate to, Long excludeAppointmentId);

    AppointmentResponse reschedule(Long appointmentId, Long currentUserId, RescheduleAppointmentRequest request);

    byte[] exportIcs(Long appointmentId, Long userId, boolean admin);

    GoogleCalendarLinkResponse googleCalendarLink(Long appointmentId, Long userId, boolean admin);
}
