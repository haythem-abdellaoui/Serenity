package com.serenity.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for POST /predict/crisis on the monitoring-ai FastAPI service.
 */
public record MonitoringAiCrisisRequest(
        @JsonProperty("patient_id") long patientId,
        @JsonProperty("avg_mood_7days") double avgMood7days,
        @JsonProperty("crisis_entries_count") int crisisEntriesCount,
        @JsonProperty("days_of_silence") int daysOfSilence,
        @JsonProperty("trigger_intensity_avg") double triggerIntensityAvg,
        @JsonProperty("mood_trend") int moodTrend,
        @JsonProperty("min_mood_7days") int minMood7days,
        @JsonProperty("trigger_count") int triggerCount,
        @JsonProperty("total_entries") int totalEntries,
        @JsonProperty("mood_description_text") String moodDescriptionText,
        @JsonProperty("trigger_text") String triggerText
) {
}
