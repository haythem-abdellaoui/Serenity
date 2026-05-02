package com.example.appointment.security;

/**
 * Stash values on the servlet request so service-layer code can reuse them after filters run.
 */
public final class AppointmentRequestAttributes {

    private AppointmentRequestAttributes() {}

    /** Full {@code Authorization} header (e.g. {@code Bearer …}) for outbound calls to user-service. */
    public static final String AUTHORIZATION_HEADER = "com.example.appointment.authorizationHeader";
}
