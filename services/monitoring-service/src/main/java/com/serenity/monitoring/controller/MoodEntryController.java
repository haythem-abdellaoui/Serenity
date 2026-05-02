package com.serenity.monitoring.controller;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.service.MoodEntryService;
import com.serenity.monitoring.service.PatientRecordExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring/mood")
@RequiredArgsConstructor
public class MoodEntryController {

    private final MoodEntryService moodEntryService;
    private final PatientRecordExportService patientRecordExportService;

    /**
     * Create a new mood entry
     * POST /api/monitoring/mood
     */
    @PostMapping
    public ResponseEntity<MoodEntryResponseDTO> createMoodEntry(@Valid @RequestBody MoodEntryRequestDTO request) {
        MoodEntryResponseDTO response = moodEntryService.createMoodEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all mood entries for a patient
     * GET /api/monitoring/mood?patientId=1
     */
    @GetMapping
    public ResponseEntity<List<MoodEntryResponseDTO>> getMoodEntriesByPatient(@RequestParam Long patientId) {
        List<MoodEntryResponseDTO> response = moodEntryService.getMoodEntriesByPatient(patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all mood entries assigned to a doctor
     * GET /api/monitoring/mood/doctor/{doctorId}
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MoodEntryResponseDTO>> getMoodEntriesByDoctor(@PathVariable Long doctorId) {
        List<MoodEntryResponseDTO> response = moodEntryService.getMoodEntriesByDoctor(doctorId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific mood entry by ID
     * GET /api/monitoring/mood/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MoodEntryResponseDTO> getMoodEntryById(@PathVariable Long id) {
        MoodEntryResponseDTO response = moodEntryService.getMoodEntryById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a mood entry
     * PUT /api/monitoring/mood/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MoodEntryResponseDTO> updateMoodEntry(@PathVariable Long id,
                                                                 @Valid @RequestBody MoodEntryRequestDTO request) {
        MoodEntryResponseDTO response = moodEntryService.updateMoodEntry(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a mood entry
     * DELETE /api/monitoring/mood/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMoodEntry(@PathVariable Long id) {
        moodEntryService.deleteMoodEntry(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Export a full patient mental health record as PDF for the assigned doctor.
     * GET /api/monitoring/mood/doctor/{doctorId}/patient/{patientId}/record-pdf
     */
    @GetMapping(value = "/doctor/{doctorId}/patient/{patientId}/record-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<byte[]> exportPatientRecordPdf(@PathVariable Long doctorId,
                                                         @PathVariable Long patientId,
                                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null || !doctorId.equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only export records for your own doctor account");
        }

        byte[] pdf = patientRecordExportService.exportDoctorPatientRecordPdf(doctorId, patientId);
        String filename = "patient-record-" + patientId + "-" + System.currentTimeMillis() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
