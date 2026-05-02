package com.example.appointment.service.impl;

import com.example.appointment.entity.*;
import com.example.appointment.integration.AppointmentMailService;
import com.example.appointment.integration.UserEmailClient;
import com.example.appointment.repository.AppointmentReminderDispatchRepository;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.AppointmentUserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentNotificationServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentUserNotificationRepository notificationRepository;
    @Mock
    private AppointmentReminderDispatchRepository dispatchRepository;
    @Mock
    private UserEmailClient userEmailClient;
    @Mock
    private AppointmentMailService appointmentMailService;

    @InjectMocks
    private AppointmentNotificationServiceImpl service;

    @Test
    void notifyPatientRequested_persistsNotification_andSendsMailWhenEmailExists() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .patientUserId(10L)
                .doctorUserId(20L)
                .appointmentDate(LocalDate.now().plusDays(2))
                .timeSlot("09:00")
                .build();
        when(userEmailClient.fetchEmailsByUserIds(List.of(20L))).thenReturn(Map.of(20L, "doc@test.com"));

        service.notifyPatientRequested(appt);

        verify(notificationRepository).save(any(AppointmentUserNotification.class));
        verify(appointmentMailService).sendReminderEmail(eq("doc@test.com"), contains("new appointment request"), contains("requested"));
    }

    @Test
    void countUnread_returnsRepositoryCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(9L)).thenReturn(4L);
        assertEquals(4L, service.countUnread(9L).getUnreadCount());
    }

    @Test
    void markAsRead_rejectsForeignNotification() {
        AppointmentUserNotification n = AppointmentUserNotification.builder()
                .id(7L)
                .userId(20L)
                .isRead(false)
                .build();
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(n));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.markAsRead(7L, 21L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void markAllAsRead_marksUnreadItemsOnly() {
        AppointmentUserNotification n1 = AppointmentUserNotification.builder().id(1L).userId(5L).isRead(false).build();
        AppointmentUserNotification n2 = AppointmentUserNotification.builder().id(2L).userId(5L).isRead(true).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(n1, n2));

        service.markAllAsRead(5L);

        assertTrue(n1.getIsRead());
        assertTrue(n2.getIsRead());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void runReminderTick_checksAllReminderKindsForConfirmedAppointments() {
        LocalTime soon = LocalTime.now().plusMinutes(6);
        String timeSlot = String.format("%02d:%02d", soon.getHour(), soon.getMinute());
        Appointment appt = Appointment.builder()
                .id(55L)
                .patientUserId(100L)
                .doctorUserId(200L)
                .appointmentDate(LocalDate.now())
                .timeSlot(timeSlot)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findByStatusFromDate(eq(AppointmentStatus.CONFIRMED), any(LocalDate.class)))
                .thenReturn(List.of(appt));
        when(dispatchRepository.existsByAppointmentIdAndReminderKind(anyLong(), any())).thenReturn(false);

        service.runReminderTick();

        verify(dispatchRepository).existsByAppointmentIdAndReminderKind(55L, AppointmentReminderKind.ONE_DAY);
        verify(dispatchRepository).existsByAppointmentIdAndReminderKind(55L, AppointmentReminderKind.FIVE_HOURS);
        verify(dispatchRepository).existsByAppointmentIdAndReminderKind(55L, AppointmentReminderKind.ONE_HOUR);
        verify(dispatchRepository).existsByAppointmentIdAndReminderKind(55L, AppointmentReminderKind.FIVE_MINUTES);
    }
}
