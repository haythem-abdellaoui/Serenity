package com.example.appointment.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class AppointmentAuth {

    private AppointmentAuth() {
    }

    public static Long requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }

    public static void requireDoctor() {
        if (!isDoctor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor only");
        }
    }

    public static void requirePatient() {
        if (!isPatient()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient only");
        }
    }

    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public static boolean isDoctor() {
        return hasRole("ROLE_DOCTOR");
    }

    public static boolean isPatient() {
        return hasRole("ROLE_PATIENT");
    }

    private static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
