package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.WeeklyDoctorDigestPayload;
import com.serenity.monitoring.dto.WeeklyDoctorDigestResponseDTO;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.entity.WeeklyDoctorDigest;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.WeeklyDoctorDigestRepository;
import com.serenity.monitoring.service.CrisisAlertService;
import com.serenity.monitoring.service.WeeklyDoctorDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyDoctorDigestServiceImpl implements WeeklyDoctorDigestService {

    private final UserAccountRepository userAccountRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final WeeklyDoctorDigestRepository weeklyDoctorDigestRepository;
    private final CrisisAlertService crisisAlertService;

    @Value("${app.digest.timezone:Africa/Tunis}")
    private String digestTimezone;

    @Override
    @Transactional
    public void generateWeeklyDigests() {
        ZoneId zoneId = ZoneId.of(digestTimezone);
        LocalDate thisWeekMonday = LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekStart = thisWeekMonday.minusWeeks(1);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<UserAccount> doctors = userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR");
        if (doctors.isEmpty()) {
            log.info("Weekly digest skipped: no active doctors found");
            return;
        }

        for (UserAccount doctor : doctors) {
            Long doctorId = doctor.getId();
            if (weeklyDoctorDigestRepository.existsByDoctorIdAndWeekStartDate(doctorId, weekStart)) {
                log.debug("Weekly digest already exists for doctorId={} weekStart={}", doctorId, weekStart);
                continue;
            }

            WeeklyDoctorDigest digest = buildDigestForDoctor(doctorId, weekStart, weekEnd, zoneId);
            weeklyDoctorDigestRepository.save(digest);

            crisisAlertService.sendWeeklyDigestNotification(
                    WeeklyDoctorDigestPayload.builder()
                            .doctorId(digest.getDoctorId())
                            .weekStartDate(digest.getWeekStartDate())
                            .weekEndDate(digest.getWeekEndDate())
                            .crisisCount(digest.getCrisisCount())
                            .worseningPatients(digest.getWorseningPatients())
                            .noCheckinPatients(digest.getNoCheckinPatients())
                            .summaryMessage(digest.getSummaryMessage())
                            .generatedAt(digest.getGeneratedAt())
                            .build()
            );
        }

        log.info("Weekly doctor digest generation completed for {} doctors", doctors.size());
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyDoctorDigestResponseDTO getLatestDigestForDoctor(Long doctorId) {
        return weeklyDoctorDigestRepository.findTopByDoctorIdOrderByWeekStartDateDesc(doctorId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyDoctorDigestResponseDTO> getRecentDigestsForDoctor(Long doctorId) {
        return weeklyDoctorDigestRepository.findTop12ByDoctorIdOrderByWeekStartDateDesc(doctorId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private WeeklyDoctorDigest buildDigestForDoctor(Long doctorId, LocalDate weekStart, LocalDate weekEnd, ZoneId zoneId) {
        Date currentStart = toDateAtStartOfDay(weekStart, zoneId);
        Date currentEnd = toDateAtEndOfDay(weekEnd, zoneId);

        LocalDate previousWeekStart = weekStart.minusWeeks(1);
        LocalDate previousWeekEnd = weekEnd.minusWeeks(1);
        Date previousStart = toDateAtStartOfDay(previousWeekStart, zoneId);
        Date previousEnd = toDateAtEndOfDay(previousWeekEnd, zoneId);

        List<MoodEntry> allEntries = moodEntryRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        List<MoodEntry> currentEntries = allEntries.stream()
                .filter(e -> !e.getCreatedAt().before(currentStart) && !e.getCreatedAt().after(currentEnd))
                .toList();
        List<MoodEntry> previousEntries = allEntries.stream()
                .filter(e -> !e.getCreatedAt().before(previousStart) && !e.getCreatedAt().after(previousEnd))
                .toList();

        int crisisCount = (int) currentEntries.stream().filter(e -> e.getMoodScore() != null && e.getMoodScore() <= 3).count();

        Map<Long, Double> currentAvgByPatient = averageMoodByPatient(currentEntries);
        Map<Long, Double> previousAvgByPatient = averageMoodByPatient(previousEntries);

        int worseningPatients = (int) currentAvgByPatient.entrySet().stream()
                .filter(entry -> previousAvgByPatient.containsKey(entry.getKey()))
                .filter(entry -> entry.getValue() + 0.3 < previousAvgByPatient.get(entry.getKey()))
                .count();

        Set<Long> assignedPatients = allEntries.stream().map(MoodEntry::getPatientId).collect(Collectors.toSet());
        Set<Long> checkinPatients = currentEntries.stream().map(MoodEntry::getPatientId).collect(Collectors.toSet());
        int noCheckinPatients = (int) assignedPatients.stream().filter(patientId -> !checkinPatients.contains(patientId)).count();

        String summary = String.format(
                "Weekly recap (%s to %s): %d crisis entries, %d worsening patients, %d patients with no check-in.",
                weekStart, weekEnd, crisisCount, worseningPatients, noCheckinPatients
        );

        return WeeklyDoctorDigest.builder()
                .doctorId(doctorId)
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .crisisCount(crisisCount)
                .worseningPatients(worseningPatients)
                .noCheckinPatients(noCheckinPatients)
                .summaryMessage(summary)
                .generatedAt(new Date())
                .build();
    }

    private Map<Long, Double> averageMoodByPatient(List<MoodEntry> entries) {
        return entries.stream().collect(Collectors.groupingBy(
                MoodEntry::getPatientId,
                Collectors.averagingDouble(e -> normalizeMoodToFiveScale(e.getMoodScore()))
        ));
    }

    private double normalizeMoodToFiveScale(Integer rawMood) {
        if (rawMood == null) {
            return 0;
        }
        double value = rawMood > 5 ? rawMood / 2.0 : rawMood;
        return Math.max(1.0, Math.min(5.0, value));
    }

    private Date toDateAtStartOfDay(LocalDate date, ZoneId zoneId) {
        return Date.from(date.atStartOfDay(zoneId).toInstant());
    }

    private Date toDateAtEndOfDay(LocalDate date, ZoneId zoneId) {
        return Date.from(date.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant());
    }

    private WeeklyDoctorDigestResponseDTO toDto(WeeklyDoctorDigest digest) {
        return WeeklyDoctorDigestResponseDTO.builder()
                .id(digest.getId())
                .doctorId(digest.getDoctorId())
                .weekStartDate(digest.getWeekStartDate())
                .weekEndDate(digest.getWeekEndDate())
                .crisisCount(digest.getCrisisCount())
                .worseningPatients(digest.getWorseningPatients())
                .noCheckinPatients(digest.getNoCheckinPatients())
                .summaryMessage(digest.getSummaryMessage())
                .generatedAt(digest.getGeneratedAt())
                .build();
    }
}

