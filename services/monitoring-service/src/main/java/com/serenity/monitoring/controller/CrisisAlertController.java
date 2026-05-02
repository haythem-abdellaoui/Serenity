package com.serenity.monitoring.controller;

import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.service.CrisisAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/monitoring/alerts")
@RequiredArgsConstructor
public class CrisisAlertController {

    private final CrisisAlertService crisisAlertService;

    @GetMapping(value = "/stream/{doctorId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    public SseEmitter subscribe(@PathVariable Long doctorId,
                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new org.springframework.security.access.AccessDeniedException("Missing authenticated doctor");
        }
        if (!doctorId.equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only subscribe to your own alert stream");
        }
        return crisisAlertService.subscribe(doctorId);
    }
}

