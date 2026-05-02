package com.serenity.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMentalHealthRecordDTO {

    private Long patientId;
    private String patientFullName;
    private String patientEmail;
    private String patientAvatarUrl;
    private String patientBio;
    private String preferredLanguage;
    private String ageDisplay;

    private Long doctorId;
    private String doctorFullName;

    private LocalDateTime generatedAt;
    private Integer totalMoodEntries;
    private Integer totalClinicalTriggers;
    private Double averageMoodScore;
    private String stabilitySummary;

    private List<MoodEntryRecordItem> moodEntries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoodEntryRecordItem {
        private Long moodEntryId;
        private Integer moodScore;
        private String moodDescription;
        private String triggers;
        private Date createdAt;
        private List<TriggerRecordItem> clinicalTriggers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TriggerRecordItem {
        private String triggerType;
        private String description;
        private Integer intensity;
        private LocalDateTime recordedAt;
    }
}

