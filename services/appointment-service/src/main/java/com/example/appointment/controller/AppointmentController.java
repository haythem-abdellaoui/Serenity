package com.example.appointment.controller;

import com.example.appointment.dto.*;
import com.example.appointment.security.AppointmentAuth;
import com.example.appointment.service.AppointmentNotificationService;
import com.example.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentNotificationService appointmentNotificationService;

    /**
     * Static paths must be registered before {@code /{id:\\d+}} so "notifications" is never captured as an id.
     */
    @GetMapping("/notifications/me")
    public ResponseEntity<List<AppointmentNotificationResponse>> getMyNotifications() {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(appointmentNotificationService.getMyNotifications(userId));
    }

    @GetMapping("/notifications/me/unread-count")
    public ResponseEntity<AppointmentUnreadCountResponse> getAppointmentNotificationsUnreadCount() {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(appointmentNotificationService.countUnread(userId));
    }

    @PatchMapping("/notifications/me/{notificationId}/read")
    public ResponseEntity<Void> markAppointmentNotificationRead(@PathVariable @Positive Long notificationId) {
        Long userId = AppointmentAuth.requireUserId();
        appointmentNotificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/notifications/me/read-all")
    public ResponseEntity<Void> markAllAppointmentNotificationsRead() {
        Long userId = AppointmentAuth.requireUserId();
        appointmentNotificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/patient-request")
    public ResponseEntity<AppointmentResponse> patientRequest(@Valid @RequestBody CreateAppointmentPatientRequest request) {
        Long userId = AppointmentAuth.requireUserId();
        AppointmentAuth.requirePatient();
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.patientRequest(userId, request));
    }

    @PostMapping("/doctor-schedule")
    public ResponseEntity<AppointmentResponse> doctorSchedule(@Valid @RequestBody CreateAppointmentDoctorRequest request) {
        Long userId = AppointmentAuth.requireUserId();
        AppointmentAuth.requireDoctor();
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.doctorSchedule(userId, request));
    }

    @PatchMapping("/{id:\\d+}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        AppointmentAuth.requireDoctor();
        return ResponseEntity.ok(appointmentService.confirm(id, userId));
    }

    @PatchMapping("/{id:\\d+}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(appointmentService.cancel(id, userId));
    }

    @PatchMapping("/{id:\\d+}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(appointmentService.reschedule(id, userId, request));
    }

    @PatchMapping("/{id:\\d+}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        AppointmentAuth.requireDoctor();
        return ResponseEntity.ok(appointmentService.complete(id, userId));
    }

    /**
     * Lists appointments: {@code GET /api/appointments/list?scope=mine} (default) or {@code scope=all} (admin).
     * Subpath {@code /list} avoids ambiguity with {@code /{id}} and matches gateway {@code Path=/api/appointments/**}.
     */
    @GetMapping("/list")
    public ResponseEntity<List<AppointmentResponse>> listAppointments(
            @RequestParam(name = "scope", defaultValue = "mine") String scope) {
        if ("all".equalsIgnoreCase(scope)) {
            AppointmentAuth.requireAdmin();
            return ResponseEntity.ok(appointmentService.listAll());
        }
        if (!"mine".equalsIgnoreCase(scope)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be mine or all");
        }
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(appointmentService.listMine(userId));
    }

    /**
     * Busy slots for calendar UI. Patients must pass {@code doctorUserId}. Doctors may pass {@code patientUserId}
     * to include that patient's other appointments (avoid double-booking).
     */
    @GetMapping("/calendar-hints")
    public ResponseEntity<List<CalendarBusySlotResponse>> calendarHints(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long doctorUserId,
            @RequestParam(required = false) @Positive Long patientUserId,
            @RequestParam(required = false) @Positive Long excludeAppointmentId) {
        Long uid = AppointmentAuth.requireUserId();
        if (AppointmentAuth.isPatient()) {
            if (doctorUserId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "doctorUserId is required");
            }
            return ResponseEntity.ok(appointmentService.calendarHintsForPatient(uid, doctorUserId, from, to, excludeAppointmentId));
        }
        if (AppointmentAuth.isDoctor()) {
            return ResponseEntity.ok(appointmentService.calendarHintsForDoctor(uid, patientUserId, from, to, excludeAppointmentId));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Calendar hints are only for patients and doctors");
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<AppointmentResponse> getOne(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        boolean admin = AppointmentAuth.isAdmin();
        return ResponseEntity.ok(appointmentService.getById(id, userId, admin));
    }

    @GetMapping(value = "/{id:\\d+}/calendar.ics", produces = "text/calendar")
    public ResponseEntity<byte[]> downloadCalendarIcs(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        boolean admin = AppointmentAuth.isAdmin();
        byte[] body = appointmentService.exportIcs(id, userId, admin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"serenity-appointment-" + id + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(body);
    }

    @GetMapping("/{id:\\d+}/google-calendar-link")
    public ResponseEntity<GoogleCalendarLinkResponse> googleCalendarLink(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        boolean admin = AppointmentAuth.isAdmin();
        return ResponseEntity.ok(appointmentService.googleCalendarLink(id, userId, admin));
    }

    @GetMapping("/{id:\\d+}/teleconsultation")
    public ResponseEntity<TeleconsultationResponse> getTele(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        boolean admin = AppointmentAuth.isAdmin();
        return ResponseEntity.ok(appointmentService.getTeleconsultation(id, userId, admin));
    }

    @PatchMapping("/{id:\\d+}/teleconsultation/start")
    public ResponseEntity<TeleconsultationResponse> startTele(@PathVariable @Positive Long id) {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(appointmentService.startTeleconsultation(id, userId));
    }
}
