package com.serenity.monitoring.controller;

import com.serenity.monitoring.dto.WeeklyDoctorDigestResponseDTO;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.service.WeeklyDoctorDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring/digests")
@RequiredArgsConstructor
public class WeeklyDoctorDigestController {

    private final WeeklyDoctorDigestService weeklyDoctorDigestService;

    @GetMapping("/doctor/{doctorId}/latest")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<WeeklyDoctorDigestResponseDTO> getLatestDigest(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ensureDoctorOwnData(doctorId, currentUser);
        WeeklyDoctorDigestResponseDTO digest = weeklyDoctorDigestService.getLatestDigestForDoctor(doctorId);
        return ResponseEntity.ok(digest);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<WeeklyDoctorDigestResponseDTO>> getRecentDigests(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ensureDoctorOwnData(doctorId, currentUser);
        return ResponseEntity.ok(weeklyDoctorDigestService.getRecentDigestsForDoctor(doctorId));
    }

    @PostMapping("/run-weekly")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ResponseEntity<Void> runNow() {
        weeklyDoctorDigestService.generateWeeklyDigests();
        return ResponseEntity.accepted().build();
    }

    private void ensureDoctorOwnData(Long doctorId, CustomUserDetails currentUser) {
        if (currentUser == null || !doctorId.equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only access your own weekly digest");
        }
    }
}

