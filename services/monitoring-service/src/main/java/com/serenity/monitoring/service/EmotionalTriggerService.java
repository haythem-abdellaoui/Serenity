package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;

import java.util.List;

public interface EmotionalTriggerService {

    /**
     * Creates an emotional trigger for a mood entry.
     *
     * @param pathMoodEntryId mood entry id from URL path
     * @param request trigger request payload
     * @return created emotional trigger
     */
    EmotionalTriggerResponse createTrigger(Long pathMoodEntryId, EmotionalTriggerRequest request);

    /**
     * Retrieves all triggers for one mood entry with ownership checks.
     *
     * @param moodEntryId mood entry id
     * @return emotional trigger list
     */
    List<EmotionalTriggerResponse> getTriggersByMoodEntryId(Long moodEntryId);

    /**
     * Retrieves one trigger by id with ownership checks.
     *
     * @param id trigger id
     * @return emotional trigger
     */
    EmotionalTriggerResponse getTriggerById(Long id);

    /**
     * Updates an emotional trigger.
     *
     * @param id trigger id
     * @param request update payload
     * @return updated trigger
     */
    EmotionalTriggerResponse updateTrigger(Long id, EmotionalTriggerRequest request);

    /**
     * Deletes an emotional trigger.
     *
     * @param id trigger id
     */
    void deleteTrigger(Long id);

    /**
     * Retrieves all triggers recorded by one doctor.
     *
     * @param doctorId doctor id from path
     * @return trigger list
     */
    List<EmotionalTriggerResponse> getTriggersByDoctorId(Long doctorId);
}

