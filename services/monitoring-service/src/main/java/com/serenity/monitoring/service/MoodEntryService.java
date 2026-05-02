package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;

import java.util.List;

public interface MoodEntryService {

    /**
     * Create a new mood entry
     */
    MoodEntryResponseDTO createMoodEntry(MoodEntryRequestDTO request);

    /**
     * Get all mood entries for a patient
     */
    List<MoodEntryResponseDTO> getMoodEntriesByPatient(Long patientId);

    /**
     * Get all mood entries assigned to a doctor.
     */
    List<MoodEntryResponseDTO> getMoodEntriesByDoctor(Long doctorId);

    /**
     * Get a specific mood entry by ID
     */
    MoodEntryResponseDTO getMoodEntryById(Long id);

    /**
     * Update an existing mood entry
     */
    MoodEntryResponseDTO updateMoodEntry(Long id, MoodEntryRequestDTO request);

    /**
     * Delete a mood entry
     */
    void deleteMoodEntry(Long id);
}
