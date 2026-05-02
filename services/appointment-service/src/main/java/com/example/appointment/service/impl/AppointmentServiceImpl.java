package com.example.appointment.service.impl;

import com.example.appointment.calendar.GoogleCalendarLinkBuilder;
import com.example.appointment.calendar.IcsCalendarHelper;
import com.example.appointment.dto.*;
import com.example.appointment.entity.*;
import com.example.appointment.integration.UserDirectoryClient;
import com.example.appointment.integration.UserLookupSnippet;
import com.example.appointment.security.AppointmentRequestAttributes;
import com.example.appointment.repository.AppointmentReminderDispatchRepository;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.TeleconsultationRepository;
import com.example.appointment.service.AppointmentNotificationService;
import com.example.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final EnumSet<AppointmentStatus> BLOCKING_STATUSES =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private static final EnumSet<AppointmentStatus> CALENDAR_BLOCKING =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    /** Must match calendar UI / teleconsultation planning (blocked end-by time). */
    private static final int SLOT_DURATION_MINUTES = 90;
    private static final int WORK_START_MINUTES = 8 * 60;
    /** Clinic closes at 20:00; appointment end (start + duration) must be <= this minute-of-day. */
    private static final int WORK_END_MINUTES = 20 * 60;

    private final AppointmentRepository appointmentRepository;
    private final TeleconsultationRepository teleconsultationRepository;
    private final AppointmentReminderDispatchRepository reminderDispatchRepository;
    private final UserDirectoryClient userDirectoryClient;
    private final AppointmentNotificationService appointmentNotificationService;

    @Value("${app.teleconsultation.default-duration-minutes:30}")
    private int defaultDurationMinutes;

    @Value("${app.teleconsultation.jitsi-room-prefix:serenity-appt}")
    private String jitsiRoomPrefix;

    @Override
    @Transactional
    public AppointmentResponse patientRequest(Long patientUserId, CreateAppointmentPatientRequest request) {
        if (patientUserId.equals(request.getDoctorUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book an appointment with yourself.");
        }
        String slot = normalizeTimeSlot(request.getTimeSlot());
        assertSchedulingRules(request.getAppointmentDate(), slot);
        assertNoOverlapForBooking(request.getDoctorUserId(), patientUserId, request.getAppointmentDate(), slot);

        Appointment appt = Appointment.builder()
                .patientUserId(patientUserId)
                .doctorUserId(request.getDoctorUserId())
                .appointmentDate(request.getAppointmentDate())
                .timeSlot(slot)
                .status(AppointmentStatus.PENDING)
                .type(request.getType())
                .notes(trimToNull(request.getNotes()))
                .build();
        appt = appointmentRepository.save(appt);
        Appointment reloaded = appointmentRepository.findById(appt.getId()).orElse(appt);
        AppointmentResponse response = toResponse(reloaded);
        appointmentNotificationService.notifyPatientRequested(reloaded);
        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse doctorSchedule(Long doctorUserId, CreateAppointmentDoctorRequest request) {
        if (doctorUserId.equals(request.getPatientUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot schedule an appointment with yourself.");
        }
        String slot = normalizeTimeSlot(request.getTimeSlot());
        assertSchedulingRules(request.getAppointmentDate(), slot);
        assertNoOverlapForBooking(doctorUserId, request.getPatientUserId(), request.getAppointmentDate(), slot);

        Appointment appt = Appointment.builder()
                .patientUserId(request.getPatientUserId())
                .doctorUserId(doctorUserId)
                .appointmentDate(request.getAppointmentDate())
                .timeSlot(slot)
                .status(AppointmentStatus.CONFIRMED)
                .type(request.getType())
                .notes(trimToNull(request.getNotes()))
                .build();
        appt = appointmentRepository.save(appt);
        if (appt.getType() == AppointmentType.TELECONSULTATION) {
            createTeleconsultationForAppointment(appt);
        }
        Appointment saved = appointmentRepository.findById(appt.getId()).orElse(appt);
        AppointmentResponse response = toResponse(saved);
        appointmentNotificationService.notifyScheduledByDoctor(saved);
        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(Long appointmentId, Long doctorUserId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appt.getDoctorUserId().equals(doctorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned doctor can confirm.");
        }
        if (appt.getStatus() != AppointmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending appointments can be confirmed.");
        }
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appt);
        if (appt.getType() == AppointmentType.TELECONSULTATION
                && teleconsultationRepository.findByAppointment_Id(appt.getId()).isEmpty()) {
            createTeleconsultationForAppointment(appt);
        }
        Appointment reloaded = reload(appt.getId());
        AppointmentResponse response = toResponse(reloaded);
        appointmentNotificationService.notifyConfirmed(reloaded);
        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long appointmentId, Long currentUserId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appt.getPatientUserId().equals(currentUserId) && !appt.getDoctorUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant of this appointment.");
        }
        if (appt.getStatus() == AppointmentStatus.CANCELLED || appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment is already closed.");
        }
        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);
        teleconsultationRepository.findByAppointment_Id(appt.getId()).ifPresent(t -> {
            if (t.getStatus() != TeleconsultationStatus.COMPLETED) {
                t.setStatus(TeleconsultationStatus.CANCELLED);
                teleconsultationRepository.save(t);
            }
        });
        return toResponse(reload(appt.getId()));
    }

    @Override
    @Transactional
    public AppointmentResponse complete(Long appointmentId, Long doctorUserId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appt.getDoctorUserId().equals(doctorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the doctor can mark complete.");
        }
        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only confirmed appointments can be completed.");
        }
        appt.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appt);
        teleconsultationRepository.findByAppointment_Id(appt.getId()).ifPresent(t -> {
            t.setStatus(TeleconsultationStatus.COMPLETED);
            if (t.getEndTime() == null) {
                t.setEndTime(Instant.now());
            }
            teleconsultationRepository.save(t);
        });
        return toResponse(reload(appt.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listMine(Long userId) {
        return toResponses(appointmentRepository.findMineSorted(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAll() {
        return toResponses(appointmentRepository.findAllSorted());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id, Long userId, boolean admin) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        assertCanAccess(appt, userId, admin);
        return toResponse(reload(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeleconsultationResponse getTeleconsultation(Long appointmentId, Long userId, boolean admin) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        assertCanAccess(appt, userId, admin);
        Teleconsultation t = teleconsultationRepository.findByAppointment_Id(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No teleconsultation for this appointment."));
        return toTeleResponse(t, appt.getStatus());
    }

    @Override
    @Transactional
    public TeleconsultationResponse startTeleconsultation(Long appointmentId, Long userId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        assertCanAccess(appt, userId, false);
        if (appt.getType() != AppointmentType.TELECONSULTATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This appointment is not a teleconsultation.");
        }
        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The visit is not active for video. Only confirmed appointments can start or join a session.");
        }
        Teleconsultation t = teleconsultationRepository.findByAppointment_Id(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No teleconsultation for this appointment."));
        if (t.getStatus() == TeleconsultationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teleconsultation was cancelled.");
        }
        if (t.getStartTime() == null) {
            t.setStartTime(Instant.now());
        }
        if (t.getStatus() == TeleconsultationStatus.SCHEDULED) {
            t.setStatus(TeleconsultationStatus.LIVE);
        }
        teleconsultationRepository.save(t);
        return toTeleResponse(t, appt.getStatus());
    }

    private Appointment reload(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    private void assertCanAccess(Appointment appt, Long userId, boolean admin) {
        if (admin) {
            return;
        }
        if (!appt.getPatientUserId().equals(userId) && !appt.getDoctorUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this appointment.");
        }
    }

    /**
     * Blocks overlapping 90-minute windows for the same doctor or the same patient on the same day.
     */
    private void assertNoOverlapForBooking(Long doctorUserId, Long patientUserId, LocalDate date, String timeSlotNormalized) {
        assertNoOverlapExcluding(null, doctorUserId, patientUserId, date, timeSlotNormalized);
    }

    /**
     * Same overlap rules as booking; {@code excludeAppointmentId} skips the appointment being moved (reschedule).
     */
    private void assertNoOverlapExcluding(
            Long excludeAppointmentId,
            Long doctorUserId,
            Long patientUserId,
            LocalDate date,
            String timeSlotNormalized) {
        int newStart = slotToMinutes(timeSlotNormalized);
        int newEnd = newStart + SLOT_DURATION_MINUTES;

        for (Appointment a : appointmentRepository.findDoctorCalendarRange(
                doctorUserId, date, date, BLOCKING_STATUSES)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) {
                continue;
            }
            int s = slotToMinutes(a.getTimeSlot());
            int e = s + SLOT_DURATION_MINUTES;
            if (intervalsOverlap(newStart, newEnd, s, e)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "That time overlaps another appointment for this doctor.");
            }
        }
        for (Appointment a : appointmentRepository.findPatientCalendarRange(
                patientUserId, date, date, BLOCKING_STATUSES)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) {
                continue;
            }
            int s = slotToMinutes(a.getTimeSlot());
            int e = s + SLOT_DURATION_MINUTES;
            if (intervalsOverlap(newStart, newEnd, s, e)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "That time overlaps another appointment for this patient.");
            }
        }
    }

    private static boolean intervalsOverlap(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    /**
     * Working hours 08:00–20:00 (visit length {@link #SLOT_DURATION_MINUTES}),
     * no past dates, and if today then start must be after "now".
     */
    private void assertSchedulingRules(LocalDate date, String timeSlotNormalized) {
        int startMin = slotToMinutes(timeSlotNormalized);
        int endMin = startMin + SLOT_DURATION_MINUTES;
        if (startMin < WORK_START_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments cannot start before 08:00.");
        }
        if (endMin > WORK_END_MINUTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointments must end by 20:00 (latest start 18:30 for a 90-minute visit).");
        }
        ZoneId z = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(z);
        LocalDate today = now.toLocalDate();
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot schedule on a past date.");
        }
        if (date.equals(today)) {
            int nowMin = now.getHour() * 60 + now.getMinute();
            if (startMin <= nowMin) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a time later than the current time.");
            }
        }
    }

    private static int slotToMinutes(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private void createTeleconsultationForAppointment(Appointment appt) {
        // One stable room per appointment so doctor and patient always join the same Jitsi URL.
        String room = jitsiRoomPrefix + "-" + appt.getId();
        String url = "https://meet.jit.si/" + room;
        Teleconsultation tele = Teleconsultation.builder()
                .appointment(appt)
                .meetingUrl(url)
                .durationMinutes(defaultDurationMinutes)
                .status(TeleconsultationStatus.SCHEDULED)
                .build();
        teleconsultationRepository.save(tele);
    }

    private static String normalizeTimeSlot(String time) {
        if (time == null || time.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeSlot is required");
        }
        String t = time.trim();
        String[] parts = t.split(":");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeSlot must be HH:mm (24h)");
        }
        int h;
        int m;
        try {
            h = Integer.parseInt(parts[0].trim());
            m = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeSlot must be HH:mm (24h)");
        }
        if (h < 0 || h > 23 || m < 0 || m > 59) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time");
        }
        return String.format("%02d:%02d", h, m);
    }

    private static String trimToNull(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }

    private String currentAuthorizationHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            var req = sra.getRequest();
            Object stored = req.getAttribute(AppointmentRequestAttributes.AUTHORIZATION_HEADER);
            if (stored instanceof String s && !s.isBlank()) {
                return s;
            }
            String h = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (h == null || h.isBlank()) {
                h = req.getHeader("authorization");
            }
            return h != null && !h.isBlank() ? h : null;
        }
        return null;
    }

    private List<AppointmentResponse> toResponses(Collection<Appointment> appointments) {
        List<AppointmentResponse> cores = appointments.stream().map(this::buildResponseCore).toList();
        if (cores.isEmpty()) {
            return cores;
        }
        Set<Long> ids = new HashSet<>();
        for (AppointmentResponse r : cores) {
            ids.add(r.getPatientUserId());
            ids.add(r.getDoctorUserId());
        }
        Map<Long, UserLookupSnippet> map = userDirectoryClient.resolveNamesById(ids, currentAuthorizationHeader());
        return cores.stream().map(r -> applyParticipantNames(r, map)).toList();
    }

    private AppointmentResponse applyParticipantNames(AppointmentResponse r, Map<Long, UserLookupSnippet> map) {
        UserLookupSnippet p = map.get(r.getPatientUserId());
        UserLookupSnippet d = map.get(r.getDoctorUserId());
        return r.toBuilder()
                .patientFirstName(p != null ? p.getFirstName() : null)
                .patientLastName(p != null ? p.getLastName() : null)
                .doctorFirstName(d != null ? d.getFirstName() : null)
                .doctorLastName(d != null ? d.getLastName() : null)
                .build();
    }

    private AppointmentResponse toResponse(Appointment a) {
        AppointmentResponse core = buildResponseCore(a);
        Map<Long, UserLookupSnippet> map = userDirectoryClient.resolveNamesById(
                List.of(core.getPatientUserId(), core.getDoctorUserId()),
                currentAuthorizationHeader());
        return applyParticipantNames(core, map);
    }

    private AppointmentResponse buildResponseCore(Appointment a) {
        Appointment fresh = appointmentRepository.findById(a.getId()).orElse(a);
        TeleconsultationResponse tele = teleconsultationRepository.findByAppointment_Id(fresh.getId())
                .map(t -> toTeleResponse(t, fresh.getStatus()))
                .orElse(null);
        return AppointmentResponse.builder()
                .id(fresh.getId())
                .patientUserId(fresh.getPatientUserId())
                .doctorUserId(fresh.getDoctorUserId())
                .appointmentDate(fresh.getAppointmentDate())
                .timeSlot(fresh.getTimeSlot())
                .status(fresh.getStatus())
                .type(fresh.getType())
                .notes(fresh.getNotes())
                .createdAt(fresh.getCreatedAt())
                .teleconsultation(tele)
                .build();
    }

    private TeleconsultationResponse toTeleResponse(Teleconsultation t, AppointmentStatus appointmentStatus) {
        boolean redactMeeting = appointmentStatus == AppointmentStatus.COMPLETED
                || appointmentStatus == AppointmentStatus.CANCELLED;
        return TeleconsultationResponse.builder()
                .id(t.getId())
                .meetingUrl(redactMeeting ? null : t.getMeetingUrl())
                .durationMinutes(t.getDurationMinutes())
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .recordingUrl(redactMeeting ? null : t.getRecordingUrl())
                .status(t.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarBusySlotResponse> calendarHintsForPatient(
            Long patientUserId, Long doctorUserId, LocalDate from, LocalDate to, Long excludeAppointmentId) {
        validateCalendarRange(from, to);
        List<CalendarBusySlotResponse> out = new ArrayList<>();
        for (Appointment a : appointmentRepository.findDoctorCalendarRange(
                doctorUserId, from, to, CALENDAR_BLOCKING)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) {
                continue;
            }
            out.add(CalendarBusySlotResponse.builder()
                    .appointmentDate(a.getAppointmentDate())
                    .timeSlot(a.getTimeSlot())
                    .source("DOCTOR")
                    .build());
        }
        for (Appointment a : appointmentRepository.findPatientCalendarRange(
                patientUserId, from, to, CALENDAR_BLOCKING)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) {
                continue;
            }
            out.add(CalendarBusySlotResponse.builder()
                    .appointmentDate(a.getAppointmentDate())
                    .timeSlot(a.getTimeSlot())
                    .source("PATIENT")
                    .build());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarBusySlotResponse> calendarHintsForDoctor(
            Long doctorUserId, Long patientUserIdOrNull, LocalDate from, LocalDate to, Long excludeAppointmentId) {
        validateCalendarRange(from, to);
        List<CalendarBusySlotResponse> out = new ArrayList<>();
        for (Appointment a : appointmentRepository.findDoctorCalendarRange(
                doctorUserId, from, to, CALENDAR_BLOCKING)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) {
                continue;
            }
            out.add(CalendarBusySlotResponse.builder()
                    .appointmentDate(a.getAppointmentDate())
                    .timeSlot(a.getTimeSlot())
                    .source("DOCTOR")
                    .build());
        }
        if (patientUserIdOrNull != null) {
            for (Appointment a : appointmentRepository.findPatientCalendarRange(
                    patientUserIdOrNull, from, to, CALENDAR_BLOCKING)) {
                if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) {
                    continue;
                }
                out.add(CalendarBusySlotResponse.builder()
                        .appointmentDate(a.getAppointmentDate())
                        .timeSlot(a.getTimeSlot())
                        .source("PATIENT")
                        .build());
            }
        }
        return out;
    }

    private static void validateCalendarRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be on or before to");
        }
        if (from.plusMonths(4).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range may not exceed 4 months.");
        }
    }

    @Override
    @Transactional
    public AppointmentResponse reschedule(Long appointmentId, Long currentUserId, RescheduleAppointmentRequest request) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appt.getPatientUserId().equals(currentUserId) && !appt.getDoctorUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the patient or doctor can reschedule.");
        }
        if (appt.getStatus() != AppointmentStatus.PENDING && appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending or confirmed appointments can be rescheduled.");
        }
        String slot = normalizeTimeSlot(request.getTimeSlot());
        assertSchedulingRules(request.getAppointmentDate(), slot);
        assertNoOverlapExcluding(appt.getId(), appt.getDoctorUserId(), appt.getPatientUserId(),
                request.getAppointmentDate(), slot);

        reminderDispatchRepository.deleteByAppointmentId(appt.getId());
        appt.setAppointmentDate(request.getAppointmentDate());
        appt.setTimeSlot(slot);
        appointmentRepository.save(appt);

        Appointment reloaded = reload(appt.getId());
        appointmentNotificationService.notifyRescheduled(reloaded);
        return toResponse(reloaded);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportIcs(Long appointmentId, Long userId, boolean admin) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        assertCanAccess(appt, userId, admin);
        if (appt.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled appointments cannot be exported to calendar.");
        }
        return IcsCalendarHelper.build(appt, SLOT_DURATION_MINUTES, ZoneId.systemDefault());
    }

    @Override
    @Transactional(readOnly = true)
    public GoogleCalendarLinkResponse googleCalendarLink(Long appointmentId, Long userId, boolean admin) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        assertCanAccess(appt, userId, admin);
        if (appt.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled appointments cannot be added to Google Calendar.");
        }
        String url = GoogleCalendarLinkBuilder.build(appt, SLOT_DURATION_MINUTES, ZoneId.systemDefault());
        return new GoogleCalendarLinkResponse(url);
    }
}
