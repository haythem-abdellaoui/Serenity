package com.example.appointment.calendar;

import com.example.appointment.entity.Appointment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * "Add to Google Calendar" template URL (no OAuth required).
 */
public final class GoogleCalendarLinkBuilder {

    private static final DateTimeFormatter G = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private GoogleCalendarLinkBuilder() {
    }

    public static String build(Appointment appt, int durationMinutes, ZoneId zone) {
        LocalDateTime start = LocalDateTime.of(appt.getAppointmentDate(), parseTime(appt.getTimeSlot()));
        LocalDateTime end = start.plus(durationMinutes, ChronoUnit.MINUTES);
        String ds = start.atZone(zone).format(G);
        String de = end.atZone(zone).format(G);
        String text = "Serenity appointment #" + appt.getId();
        String details = "Type: " + appt.getType() + " · Status: " + appt.getStatus();
        String dates = ds + "/" + de;
        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + enc(text)
                + "&dates=" + enc(dates)
                + "&details=" + enc(details);
    }

    private static java.time.LocalTime parseTime(String hhmm) {
        String[] p = hhmm.split(":");
        return java.time.LocalTime.of(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
