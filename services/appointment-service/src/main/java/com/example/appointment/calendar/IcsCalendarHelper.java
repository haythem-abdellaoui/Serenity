package com.example.appointment.calendar;

import com.example.appointment.entity.Appointment;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Minimal iCalendar (RFC 5545) export for "Add to calendar" flows.
 */
public final class IcsCalendarHelper {

    private static final DateTimeFormatter ICS_TS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private IcsCalendarHelper() {
    }

    public static byte[] build(Appointment appt, int durationMinutes, ZoneId zone) {
        LocalDateTime start = LocalDateTime.of(appt.getAppointmentDate(), parseTime(appt.getTimeSlot()));
        LocalDateTime end = start.plus(durationMinutes, ChronoUnit.MINUTES);
        String uid = "serenity-appt-" + appt.getId() + "@serenity";
        String summary = escapeText("Serenity — appointment #" + appt.getId());
        String desc = escapeText("Type: " + appt.getType() + ". Status: " + appt.getStatus());
        String dtStart = start.atZone(zone).format(ICS_TS);
        String dtEnd = end.atZone(zone).format(ICS_TS);
        String now = java.time.ZonedDateTime.now(zone).format(ICS_TS);

        String body = "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//Serenity//Appointment//EN\r\n"
                + "CALSCALE:GREGORIAN\r\n"
                + "METHOD:PUBLISH\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + uid + "\r\n"
                + "DTSTAMP:" + now + "\r\n"
                + "DTSTART:" + dtStart + "\r\n"
                + "DTEND:" + dtEnd + "\r\n"
                + "SUMMARY:" + summary + "\r\n"
                + "DESCRIPTION:" + desc + "\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private static java.time.LocalTime parseTime(String hhmm) {
        String[] p = hhmm.split(":");
        int h = Integer.parseInt(p[0].trim());
        int m = Integer.parseInt(p[1].trim());
        return java.time.LocalTime.of(h, m);
    }

    private static String escapeText(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
