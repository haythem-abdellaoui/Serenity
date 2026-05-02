package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.MonitoringAiCrisisRequest;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the 7-day rolling feature vector expected by monitoring-ai /predict/crisis.
 */
@Service
@RequiredArgsConstructor
public class MoodRiskFeatureService {

    private final EmotionalTriggerRepository emotionalTriggerRepository;

    public MonitoringAiCrisisRequest buildRequest(MoodEntry latestEntry) {
        Long patientId = latestEntry.getPatientId();
        List<EmotionalTrigger> triggers = latestEntry.getId() == null
                ? List.of()
                : emotionalTriggerRepository.findByMoodEntryId(latestEntry.getId());

        double triggerIntensityAvg = triggers.isEmpty()
                ? 0.0
                : triggers.stream().mapToInt(EmotionalTrigger::getIntensity).average().orElse(0.0);
        int triggerCount = triggers.size();

        int moodScore = latestEntry.getMoodScore() == null ? 5 : latestEntry.getMoodScore();
        int crisisCount = moodScore <= 3 ? 1 : 0;
        int daysOfSilence = 0;
        int moodTrend = 0;
        int totalEntries = 1;

        String moodDescriptionText = buildMoodTextContext(latestEntry);
        String triggerText = buildTriggerTextContext(latestEntry, triggers);

        return new MonitoringAiCrisisRequest(
                patientId,
                round2(moodScore),
                crisisCount,
                daysOfSilence,
                round2(triggerIntensityAvg),
                moodTrend,
                moodScore,
                triggerCount,
                totalEntries,
                moodDescriptionText,
                triggerText
        );
    }

    private String buildMoodTextContext(MoodEntry latestEntry) {
        // Per-card text classification: use only the current card description.
        String joined = latestEntry.getMoodDescription() == null
                ? ""
                : latestEntry.getMoodDescription().trim();

        int maxLen = 1500;
        if (joined.length() > maxLen) {
            return joined.substring(0, maxLen);
        }
        return joined;
    }

    private String buildTriggerTextContext(MoodEntry latestEntry, List<EmotionalTrigger> currentEntryTriggers) {
        String moodTriggerText = latestEntry.getTriggers() == null ? "" : latestEntry.getTriggers().trim();

        String clinicalTriggerText = currentEntryTriggers.stream()
                .map(t -> {
                    String type = t.getTriggerType() != null ? t.getTriggerType().trim() : "";
                    String desc = t.getDescription() != null ? t.getDescription().trim() : "";
                    if (type.isBlank()) {
                        return desc;
                    }
                    if (desc.isBlank()) {
                        return type;
                    }
                    return type + ": " + desc;
                })
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" || "));

        String joined;
        if (moodTriggerText.isBlank()) {
            joined = clinicalTriggerText;
        } else if (clinicalTriggerText.isBlank()) {
            joined = moodTriggerText;
        } else {
            joined = moodTriggerText + " || " + clinicalTriggerText;
        }

        int maxLen = 1200;
        if (joined.length() > maxLen) {
            return joined.substring(0, maxLen);
        }
        return joined;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
