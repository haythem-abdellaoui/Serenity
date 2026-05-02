package com.example.appointment.service.impl;

import com.example.appointment.dto.CreateAppointmentPatientRequest;
import com.example.appointment.dto.RescheduleAppointmentRequest;
import com.example.appointment.entity.*;
import com.example.appointment.integration.UserDirectoryClient;
import com.example.appointment.integration.UserLookupSnippet;
import com.example.appointment.repository.AppointmentReminderDispatchRepository;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.TeleconsultationRepository;
import com.example.appointment.service.AppointmentNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TeleconsultationRepository teleconsultationRepository;
    @Mock
    private AppointmentReminderDispatchRepository reminderDispatchRepository;
    @Mock
    private UserDirectoryClient userDirectoryClient;
    @Mock
    private AppointmentNotificationService appointmentNotificationService;

    @InjectMocks
    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultDurationMinutes", 30);
        ReflectionTestUtils.setField(service, "jitsiRoomPrefix", "serenity-appt");
    }

    @Test
    void patientRequest_savesPendingAppointment_andNotifies() {
        Long patientId = 10L;
        Long doctorId = 20L;
        CreateAppointmentPatientRequest request = CreateAppointmentPatientRequest.builder()
                .doctorUserId(doctorId)
                .appointmentDate(LocalDate.now().plusDays(2))
                .timeSlot("09:00")
                .type(AppointmentType.IN_PERSON)
                .notes(" checkup ")
                .build();

        Appointment saved = Appointment.builder()
                .id(1L)
                .patientUserId(patientId)
                .doctorUserId(doctorId)
                .appointmentDate(request.getAppointmentDate())
                .timeSlot("09:00")
                .status(AppointmentStatus.PENDING)
                .type(AppointmentType.IN_PERSON)
                .notes("checkup")
                .build();

        when(appointmentRepository.findDoctorCalendarRange(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findPatientCalendarRange(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(saved));
        when(teleconsultationRepository.findByAppointment_Id(1L)).thenReturn(Optional.empty());
        when(userDirectoryClient.resolveNamesById(anyCollection(), any())).thenReturn(Map.of());

        var response = service.patientRequest(patientId, request);

        assertEquals(AppointmentStatus.PENDING, response.getStatus());
        assertEquals("checkup", response.getNotes());
        verify(appointmentNotificationService).notifyPatientRequested(saved);
    }

    @Test
    void patientRequest_rejectsBookingWithSelf() {
        CreateAppointmentPatientRequest request = CreateAppointmentPatientRequest.builder()
                .doctorUserId(10L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .timeSlot("10:00")
                .type(AppointmentType.IN_PERSON)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.patientRequest(10L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void confirm_createsTeleconsultationWhenMissing() {
        Appointment appt = Appointment.builder()
                .id(7L)
                .patientUserId(11L)
                .doctorUserId(22L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .timeSlot("11:00")
                .status(AppointmentStatus.PENDING)
                .type(AppointmentType.TELECONSULTATION)
                .build();

        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appt), Optional.of(appt));
        when(teleconsultationRepository.findByAppointment_Id(7L)).thenReturn(Optional.empty(), Optional.empty());
        when(userDirectoryClient.resolveNamesById(anyCollection(), any())).thenReturn(Map.of());

        service.confirm(7L, 22L);

        ArgumentCaptor<Teleconsultation> captor = ArgumentCaptor.forClass(Teleconsultation.class);
        verify(teleconsultationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMeetingUrl().contains("serenity-appt-7"));
        verify(appointmentNotificationService).notifyConfirmed(appt);
    }

    @Test
    void getById_forbiddenForUnrelatedUser() {
        Appointment appt = Appointment.builder()
                .id(3L)
                .patientUserId(1L)
                .doctorUserId(2L)
                .status(AppointmentStatus.PENDING)
                .type(AppointmentType.IN_PERSON)
                .appointmentDate(LocalDate.now().plusDays(3))
                .timeSlot("12:00")
                .build();
        when(appointmentRepository.findById(3L)).thenReturn(Optional.of(appt));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getById(3L, 99L, false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void reschedule_deletesReminderDispatches_andUpdatesSlot() {
        Appointment appt = Appointment.builder()
                .id(5L)
                .patientUserId(9L)
                .doctorUserId(4L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .timeSlot("13:00")
                .status(AppointmentStatus.CONFIRMED)
                .type(AppointmentType.IN_PERSON)
                .build();
        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest();
        req.setAppointmentDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("15:00");

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt), Optional.of(appt), Optional.of(appt));
        when(appointmentRepository.findDoctorCalendarRange(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findPatientCalendarRange(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teleconsultationRepository.findByAppointment_Id(5L)).thenReturn(Optional.empty());
        when(userDirectoryClient.resolveNamesById(anyCollection(), any())).thenReturn(Map.of());

        var response = service.reschedule(5L, 9L, req);

        verify(reminderDispatchRepository).deleteByAppointmentId(5L);
        verify(appointmentNotificationService).notifyRescheduled(appt);
        assertEquals("15:00", response.getTimeSlot());
    }

    @Test
    void calendarHintsForDoctor_rejectsOversizedRange() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.calendarHintsForDoctor(1L, null, LocalDate.now(), LocalDate.now().plusMonths(5), null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
