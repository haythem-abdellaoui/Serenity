package com.example.appointment.controller;

import com.example.appointment.dto.GoogleCalendarAuthorizeUrlResponse;
import com.example.appointment.dto.GoogleCalendarOAuthCompleteRequest;
import com.example.appointment.dto.GoogleCalendarStatusResponse;
import com.example.appointment.dto.GoogleCalendarSyncResponse;
import com.example.appointment.security.AppointmentAuth;
import com.example.appointment.service.GoogleCalendarIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarIntegrationService googleCalendarIntegrationService;

    @GetMapping("/status")
    public ResponseEntity<GoogleCalendarStatusResponse> status() {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(googleCalendarIntegrationService.status(userId));
    }

    @GetMapping("/oauth2/authorize-url")
    public ResponseEntity<GoogleCalendarAuthorizeUrlResponse> authorizeUrl(
            @RequestParam(name = "returnTo", required = false) String returnTo) {
        AppointmentAuth.requireUserId();
        return ResponseEntity.ok(googleCalendarIntegrationService.buildAuthorizeUrl(returnTo));
    }

    @PostMapping("/oauth2/complete")
    public ResponseEntity<Void> oauthComplete(@Valid @RequestBody GoogleCalendarOAuthCompleteRequest body) {
        Long userId = AppointmentAuth.requireUserId();
        googleCalendarIntegrationService.completeOAuth(userId, body);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<GoogleCalendarSyncResponse> sync() {
        Long userId = AppointmentAuth.requireUserId();
        return ResponseEntity.ok(googleCalendarIntegrationService.syncForCurrentUser(userId));
    }
}
