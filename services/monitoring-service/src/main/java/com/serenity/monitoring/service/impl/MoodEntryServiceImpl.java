package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.dto.CrisisAlertPayload;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.mapper.MoodEntryMapper;
import com.serenity.monitoring.entity.UserProfileSnapshot;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.UserProfileSnapshotRepository;
import com.serenity.monitoring.dto.MonitoringAiCrisisResponse;
import com.serenity.monitoring.integration.MonitoringAiCrisisClient;
import com.serenity.monitoring.service.DoctorAssignmentService;
import com.serenity.monitoring.service.CrisisAlertService;
import com.serenity.monitoring.service.MoodEntryService;
import com.serenity.monitoring.service.MoodRiskFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MoodEntryServiceImpl implements MoodEntryService {

    private final MoodEntryRepository moodEntryRepository;
    private final MoodEntryMapper moodEntryMapper;
    private final DoctorAssignmentService doctorAssignmentService;
    private final CrisisAlertService crisisAlertService;
    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileSnapshotRepository userProfileSnapshotRepository;
    private final MoodRiskFeatureService moodRiskFeatureService;
    private final MonitoringAiCrisisClient monitoringAiCrisisClient;

    @Override
    public MoodEntryResponseDTO createMoodEntry(MoodEntryRequestDTO request) {
        // Get or assign doctor for this patient (Round-Robin algorithm)
        Long assignedDoctorId = doctorAssignmentService.getOrAssignDoctor(request.getPatientId());
        log.info("Creating mood entry for patientId={} assignedDoctorId={} moodScore={}",
                request.getPatientId(), assignedDoctorId, request.getMoodScore());
        
        // Set the doctor ID in the request
        request.setDoctorId(assignedDoctorId);
        
        MoodEntry moodEntry = moodEntryMapper.toEntity(request);
        MoodEntry savedEntry = moodEntryRepository.save(moodEntry);

        if (savedEntry.getMoodScore() != null && savedEntry.getMoodScore() <= 3) {
            UserAccount patient = userAccountRepository.findById(savedEntry.getPatientId()).orElse(null);
            String patientName = patient != null ? buildDisplayName(patient) : "Unknown Patient";

            CrisisAlertPayload payload = CrisisAlertPayload.builder()
                    .doctorId(savedEntry.getDoctorId())
                    .patientId(savedEntry.getPatientId())
                    .patientFullName(patientName)
                    .moodLevel(savedEntry.getMoodScore())
                    .message("Crisis alert: " + patientName + " submitted a low mood score")
                    .timestamp(savedEntry.getCreatedAt() != null ? savedEntry.getCreatedAt() : new Date())
                    .build();

            crisisAlertService.sendCrisisAlert(payload);
        } else {
            log.debug("Mood entry {} not considered crisis (moodScore={})",
                    savedEntry.getId(), savedEntry.getMoodScore());
        }

        applyAiRiskPrediction(savedEntry);

        return enrichResponse(moodEntryMapper.toResponseDTO(savedEntry));
    }

    private void applyAiRiskPrediction(MoodEntry savedEntry) {
        try {
            var request = moodRiskFeatureService.buildRequest(savedEntry);
            Optional<MonitoringAiCrisisResponse> ai = monitoringAiCrisisClient.predict(request);
            if (ai.isEmpty()) {
                return;
            }
            MonitoringAiCrisisResponse r = ai.get();
            savedEntry.setAiRiskLevel(r.riskLevel());
            savedEntry.setAiRiskConfidence(r.confidence());
            savedEntry.setAiRiskRecommendation(r.recommendation());
            String riskType = r.riskType() != null && !r.riskType().isBlank() ? r.riskType() : r.mediumRiskType();
            savedEntry.setAiRiskType(riskType);
            savedEntry.setAiMediumRiskType("MEDIUM_RISK".equals(r.riskLevel()) ? riskType : null);
            savedEntry.setAiRiskScore(r.riskScore());
            moodEntryRepository.save(savedEntry);
            log.debug("AI risk for mood entry {}: {} (confidence={})",
                    savedEntry.getId(), r.riskLevel(), r.confidence());
        } catch (Exception ex) {
            log.warn("AI risk prediction skipped for mood entry {}: {}", savedEntry.getId(), ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodEntryResponseDTO> getMoodEntriesByPatient(Long patientId) {
        List<MoodEntry> entries = moodEntryRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return enrichResponseList(moodEntryMapper.toResponseDTOList(entries));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodEntryResponseDTO> getMoodEntriesByDoctor(Long doctorId) {
        List<MoodEntry> entries = moodEntryRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        return enrichResponseList(moodEntryMapper.toResponseDTOList(entries));
    }

    @Override
    @Transactional(readOnly = true)
    public MoodEntryResponseDTO getMoodEntryById(Long id) {
        MoodEntry entry = moodEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood entry not found with ID: " + id));
        return enrichResponse(moodEntryMapper.toResponseDTO(entry));
    }

    @Override
    public MoodEntryResponseDTO updateMoodEntry(Long id, MoodEntryRequestDTO request) {
        MoodEntry entry = moodEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood entry not found with ID: " + id));

        // Enforce doctor continuity and ownership continuity for this entry.
        // Client payload must never reassign doctor/patient for an existing mood entry.
        Long originalPatientId = entry.getPatientId();
        Long originalDoctorId = entry.getDoctorId();

        moodEntryMapper.updateEntityFromDTO(request, entry);
        entry.setPatientId(originalPatientId);
        entry.setDoctorId(originalDoctorId);

        MoodEntry updatedEntry = moodEntryRepository.save(entry);
        return enrichResponse(moodEntryMapper.toResponseDTO(updatedEntry));
    }

    @Override
    public void deleteMoodEntry(Long id) {
        if (!moodEntryRepository.existsById(id)) {
            throw new IllegalArgumentException("Mood entry not found with ID: " + id);
        }

        if (emotionalTriggerRepository.existsByMoodEntryId(id)) {
            throw new IllegalStateException("Cannot delete this mood entry because it has linked clinical records.");
        }

        moodEntryRepository.deleteById(id);
    }

    private List<MoodEntryResponseDTO> enrichResponseList(List<MoodEntryResponseDTO> responses) {
        if (responses == null || responses.isEmpty()) {
            return responses;
        }

        Set<Long> userIds = new HashSet<>();
        for (MoodEntryResponseDTO response : responses) {
            if (response.getPatientId() != null) {
                userIds.add(response.getPatientId());
            }
            if (response.getDoctorId() != null) {
                userIds.add(response.getDoctorId());
            }
        }

        Map<Long, UserAccount> usersById = new HashMap<>();
        userAccountRepository.findAllById(userIds).forEach(user -> usersById.put(user.getId(), user));

        Set<Long> patientIds = responses.stream()
                .map(MoodEntryResponseDTO::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserProfileSnapshot> profileByPatientId = new HashMap<>();
        if (!patientIds.isEmpty()) {
            userProfileSnapshotRepository.findAllByUserIdIn(patientIds)
                    .forEach(profile -> profileByPatientId.put(profile.getUserId(), profile));
        }

        for (MoodEntryResponseDTO response : responses) {
            UserAccount patient = usersById.get(response.getPatientId());
            if (patient != null) {
                response.setPatientName(buildDisplayName(patient));
            }
            UserProfileSnapshot profile = profileByPatientId.get(response.getPatientId());
            if (profile != null && profile.getAvatar() != null && !profile.getAvatar().isBlank()) {
                response.setPatientAvatarUrl(profile.getAvatar().trim());
            }
            UserAccount doctor = usersById.get(response.getDoctorId());
            if (doctor != null) {
                response.setDoctorName(buildDisplayName(doctor));
            }
        }

        return responses;
    }

    private MoodEntryResponseDTO enrichResponse(MoodEntryResponseDTO response) {
        if (response == null) {
            return null;
        }
        return enrichResponseList(List.of(response)).get(0);
    }

    private String buildDisplayName(UserAccount user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }
}
