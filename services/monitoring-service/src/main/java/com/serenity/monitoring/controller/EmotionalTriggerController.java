package com.serenity.monitoring.controller;

import com.serenity.monitoring.dto.ApiResponse;
import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;
import com.serenity.monitoring.service.EmotionalTriggerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class EmotionalTriggerController {

    private final EmotionalTriggerService emotionalTriggerService;

    @PostMapping("/mood/{moodEntryId}/triggers")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<EmotionalTriggerResponse>> createTrigger(
            @PathVariable Long moodEntryId,
            @Valid @RequestBody EmotionalTriggerRequest request
    ) {
        EmotionalTriggerResponse response = emotionalTriggerService.createTrigger(moodEntryId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Emotional trigger recorded successfully", response));
    }

    @GetMapping("/mood/{moodEntryId}/triggers")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<EmotionalTriggerResponse>>> getTriggersByMoodEntryId(
            @PathVariable Long moodEntryId
    ) {
        List<EmotionalTriggerResponse> response = emotionalTriggerService.getTriggersByMoodEntryId(moodEntryId);
        return ResponseEntity.ok(ApiResponse.success("Triggers retrieved successfully", response));
    }

    @GetMapping("/triggers/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<EmotionalTriggerResponse>> getTriggerById(@PathVariable Long id) {
        EmotionalTriggerResponse response = emotionalTriggerService.getTriggerById(id);
        return ResponseEntity.ok(ApiResponse.success("Trigger retrieved successfully", response));
    }

    @PutMapping("/triggers/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<EmotionalTriggerResponse>> updateTrigger(
            @PathVariable Long id,
            @Valid @RequestBody EmotionalTriggerRequest request
    ) {
        EmotionalTriggerResponse response = emotionalTriggerService.updateTrigger(id, request);
        return ResponseEntity.ok(ApiResponse.success("Emotional trigger updated successfully", response));
    }

    @DeleteMapping("/triggers/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteTrigger(@PathVariable Long id) {
        emotionalTriggerService.deleteTrigger(id);
        return ResponseEntity.ok(ApiResponse.success("Emotional trigger deleted successfully", null));
    }

    @GetMapping("/triggers/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<EmotionalTriggerResponse>>> getTriggersByDoctorId(
            @PathVariable Long doctorId
    ) {
        List<EmotionalTriggerResponse> response = emotionalTriggerService.getTriggersByDoctorId(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Triggers retrieved successfully", response));
    }
}

