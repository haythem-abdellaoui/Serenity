package com.example.appointment.service;

import com.example.appointment.config.GoogleCalendarProperties;
import com.example.appointment.dto.GoogleCalendarAuthorizeUrlResponse;
import com.example.appointment.dto.GoogleCalendarOAuthCompleteRequest;
import com.example.appointment.dto.GoogleCalendarStatusResponse;
import com.example.appointment.dto.GoogleCalendarSyncResponse;
import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.AppointmentStatus;
import com.example.appointment.entity.AppointmentType;
import com.example.appointment.entity.GoogleCalendarCredential;
import com.example.appointment.entity.GoogleCalendarEventLink;
import com.example.appointment.integration.UserDirectoryClient;
import com.example.appointment.integration.UserLookupSnippet;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.GoogleCalendarCredentialRepository;
import com.example.appointment.repository.GoogleCalendarEventLinkRepository;
import com.example.appointment.security.AppointmentRequestAttributes;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarIntegrationService {

    private static final int SLOT_DURATION_MINUTES = 90;
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleCalendarProperties properties;
    private final GoogleCalendarCredentialRepository credentialRepository;
    private final GoogleCalendarEventLinkRepository eventLinkRepository;
    private final GoogleCalendarEventLinkSaver eventLinkSaver;
    private final AppointmentRepository appointmentRepository;
    private final UserDirectoryClient userDirectoryClient;

    public boolean isConfigured() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getClientId())
                && StringUtils.hasText(properties.getClientSecret())
                && StringUtils.hasText(properties.getRedirectUri());
    }

    public GoogleCalendarStatusResponse status(Long userId) {
        if (!isConfigured()) {
            return GoogleCalendarStatusResponse.builder()
                    .connected(false)
                    .configured(false)
                    .googleEmail(null)
                    .build();
        }
        boolean linked = credentialRepository.findById(userId).isPresent();
        return GoogleCalendarStatusResponse.builder()
                .configured(true)
                .connected(linked)
                .googleEmail(null)
                .build();
    }

    public GoogleCalendarAuthorizeUrlResponse buildAuthorizeUrl(String returnTo) {
        assertConfigured();
        String safeReturn = validateReturnTo(returnTo);
        String state = URLEncoder.encode(
                Base64.getEncoder().encodeToString(safeReturn.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        String scope = URLEncoder.encode(
                String.join(" ",
                        "openid",
                        "email",
                        CalendarScopes.CALENDAR_EVENTS),
                StandardCharsets.UTF_8);
        String redirect = URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8);
        String client = URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8);
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + client
                + "&redirect_uri=" + redirect
                + "&response_type=code"
                + "&scope=" + scope
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&state=" + state;
        return GoogleCalendarAuthorizeUrlResponse.builder()
                .authorizeUrl(url)
                .returnTo(safeReturn)
                .build();
    }

    /**
     * After OAuth, Angular navigates here (must match Google redirect URI host path) then routes to this path.
     */
    public static String decodeReturnTo(String stateParam) {
        if (stateParam == null || stateParam.isBlank()) {
            return "/appointments/list";
        }
        try {
            String decoded = URLDecoder.decode(stateParam, StandardCharsets.UTF_8);
            byte[] raw = Base64.getDecoder().decode(decoded);
            return validateReturnTo(new String(raw, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return "/appointments/list";
        }
    }

    private static String validateReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return "/appointments/list";
        }
        String t = returnTo.trim();
        if ("/appointments".equals(t) || "/appointments/list".equals(t) || "/admin/appointments/list".equals(t)) {
            return t;
        }
        return "/appointments/list";
    }

    @Transactional
    public void completeOAuth(Long userId, GoogleCalendarOAuthCompleteRequest body) {
        assertConfigured();
        if (body.getCode() == null || body.getCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing authorization code");
        }
        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            var tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    JSON_FACTORY,
                    properties.getClientId(),
                    properties.getClientSecret(),
                    body.getCode(),
                    properties.getRedirectUri())
                    .execute();
            String refresh = tokenResponse.getRefreshToken();
            if (!StringUtils.hasText(refresh)) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Google did not return a refresh token. Try again and ensure you grant calendar access "
                                + "(revoke app access in Google Account settings if needed).");
            }
            GoogleCalendarCredential cred = GoogleCalendarCredential.builder()
                    .userId(userId)
                    .refreshToken(refresh)
                    .linkedAt(Instant.now())
                    .build();
            credentialRepository.save(cred);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google OAuth token exchange failed: {}", e.getMessage());
            throw new ResponseStatusException(BAD_REQUEST, "Could not complete Google sign-in: " + e.getMessage());
        }
    }

    public GoogleCalendarSyncResponse syncForCurrentUser(Long userId) {
        assertConfigured();
        GoogleCalendarCredential cred = credentialRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(PRECONDITION_FAILED,
                        "Connect Google Calendar first (use Synchronize with Google Calendar)."));
        try {
            Calendar calendar = buildCalendarClient(cred.getRefreshToken());
            List<Appointment> appointments = loadAppointmentsForUser(userId);
            int n = 0;
            List<String> failures = new ArrayList<>();
            for (Appointment appt : appointments) {
                try {
                    upsertEvent(calendar, userId, appt);
                    n++;
                    log.info("Google Calendar: synced appointment id={} for userId={}", appt.getId(), userId);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    log.warn("Google Calendar: failed appointment id={} userId={}: {}", appt.getId(), userId, msg, e);
                    failures.add("Visit #" + appt.getId() + ": " + msg);
                }
            }
            if (n == 0 && !appointments.isEmpty() && !failures.isEmpty()) {
                throw new ResponseStatusException(BAD_GATEWAY,
                        "Could not add events to Google Calendar. " + failures.get(0));
            }
            return GoogleCalendarSyncResponse.builder()
                    .eventsUpserted(n)
                    .totalCandidates(appointments.size())
                    .build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google Calendar sync failed: {}", e.getMessage());
            throw new ResponseStatusException(BAD_GATEWAY, "Google Calendar sync failed: " + e.getMessage());
        }
    }

    private void upsertEvent(Calendar calendar, Long userId, Appointment appt) throws java.io.IOException {
        Event event = buildEvent(appt, userId);
        Optional<GoogleCalendarEventLink> existing = eventLinkRepository.findByUserIdAndAppointmentId(userId, appt.getId());
        if (existing.isPresent()) {
            String eid = existing.get().getGoogleEventId();
            try {
                calendar.events().update("primary", eid, event).execute();
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex) {
                if (ex.getStatusCode() == 404) {
                    eventLinkRepository.delete(existing.get());
                    Event created = calendar.events().insert("primary", event).execute();
                    eventLinkSaver.replaceMapping(userId, appt.getId(), created.getId());
                } else {
                    throw ex;
                }
            }
        } else {
            Event created = calendar.events().insert("primary", event).execute();
            eventLinkSaver.replaceMapping(userId, appt.getId(), created.getId());
        }
    }

    private Event buildEvent(Appointment appt, Long viewerUserId) {
        ZoneId zone = ZoneId.systemDefault();
        int[] hm = parseTimeSlotToHm(appt.getTimeSlot());
        ZonedDateTime startZ = appt.getAppointmentDate()
                .atTime(hm[0], hm[1], 0)
                .atZone(zone);
        ZonedDateTime endZ = startZ.plusMinutes(SLOT_DURATION_MINUTES);
        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startZ.toInstant().toEpochMilli()))
                .setTimeZone(zone.getId());
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endZ.toInstant().toEpochMilli()))
                .setTimeZone(zone.getId());

        Map<Long, UserLookupSnippet> names = resolveParticipants(appt);
        String patientLabel = label(names.get(appt.getPatientUserId()), "Patient");
        String doctorLabel = label(names.get(appt.getDoctorUserId()), "Doctor");
        boolean asPatient = Objects.equals(viewerUserId, appt.getPatientUserId());
        String summary = asPatient
                ? "Serenity: visit with " + doctorLabel
                : "Serenity: " + patientLabel;

        String typeLine = appt.getType() == AppointmentType.TELECONSULTATION ? "Video visit" : "In person";
        String body = "Serenity appointment #" + appt.getId() + "\n"
                + "Status: " + appt.getStatus() + "\n"
                + "Type: " + typeLine + "\n"
                + "Patient: " + patientLabel + "\n"
                + "Doctor: " + doctorLabel;
        if (StringUtils.hasText(appt.getNotes())) {
            body += "\nNotes: " + appt.getNotes();
        }

        return new Event()
                .setSummary(summary)
                .setDescription(body)
                .setStart(start)
                .setEnd(end);
    }

    private Map<Long, UserLookupSnippet> resolveParticipants(Appointment appt) {
        String auth = currentAuthorizationHeader();
        return userDirectoryClient.resolveNamesById(
                List.of(appt.getPatientUserId(), appt.getDoctorUserId()), auth);
    }

    private static String label(UserLookupSnippet u, String fallback) {
        if (u == null) {
            return fallback;
        }
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String s = (fn + " " + ln).trim();
        return s.isEmpty() ? fallback : s;
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

    private List<Appointment> loadAppointmentsForUser(Long userId) {
        LocalDate min = LocalDate.now().minusDays(1);
        List<Appointment> out = new ArrayList<>();
        for (Appointment a : appointmentRepository.findMineSorted(userId)) {
            if (includeInSync(a, min)) {
                out.add(a);
            }
        }
        out.sort(Comparator
                .comparing(Appointment::getAppointmentDate)
                .thenComparing(Appointment::getTimeSlot, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    private static boolean includeInSync(Appointment a, LocalDate min) {
        if (a.getStatus() != AppointmentStatus.PENDING && a.getStatus() != AppointmentStatus.CONFIRMED) {
            return false;
        }
        return !a.getAppointmentDate().isBefore(min);
    }

    /**
     * Parses a time like "9:00", "09:00", "14:30", "09:30:00" (24h; seconds ignored if present).
     */
    private static int[] parseTimeSlotToHm(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            throw new IllegalArgumentException("time slot is required");
        }
        String[] p = timeSlot.trim().split(":");
        if (p.length < 2) {
            throw new IllegalArgumentException("Invalid time: " + timeSlot);
        }
        int h = Integer.parseInt(p[0].trim());
        int m = Integer.parseInt(p[1].trim());
        if (h < 0 || h > 23 || m < 0 || m > 59) {
            throw new IllegalArgumentException("Invalid time: " + timeSlot);
        }
        return new int[] {h, m};
    }

    private Calendar buildCalendarClient(String refreshToken) throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(properties.getClientId())
                .setClientSecret(properties.getClientSecret())
                .setRefreshToken(refreshToken)
                .build();
        return new Calendar.Builder(transport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Serenity Appointments")
                .build();
    }

    private void assertConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Google Calendar is not configured: set GOOGLE_CALENDAR_CLIENT_ID and GOOGLE_CALENDAR_CLIENT_SECRET, or GOOGLE_CALENDAR_CREDENTIALS_JSON.");
        }
    }
}
