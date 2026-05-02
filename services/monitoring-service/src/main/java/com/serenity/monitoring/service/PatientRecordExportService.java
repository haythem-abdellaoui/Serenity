package com.serenity.monitoring.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.serenity.monitoring.dto.PatientMentalHealthRecordDTO;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.entity.UserProfileSnapshot;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.UserProfileSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientRecordExportService {

    private final MoodEntryRepository moodEntryRepository;
    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileSnapshotRepository userProfileSnapshotRepository;

    @Transactional(readOnly = true)
    public byte[] exportDoctorPatientRecordPdf(Long doctorId, Long patientId) {
        List<MoodEntry> moodEntries = moodEntryRepository
                .findByDoctorIdAndPatientIdOrderByCreatedAtDesc(doctorId, patientId);

        if (moodEntries.isEmpty()) {
            throw new IllegalArgumentException("No mood entries found for this patient under your care.");
        }

        UserAccount patient = userAccountRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        UserAccount doctor = userAccountRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        UserProfileSnapshot profile = userProfileSnapshotRepository.findByUserId(patientId).orElse(null);

        List<Long> moodEntryIds = moodEntries.stream()
                .map(MoodEntry::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<EmotionalTrigger>> triggersByMoodEntryId = moodEntryIds.isEmpty()
                ? Collections.emptyMap()
                : emotionalTriggerRepository.findAllForExportByMoodEntryIds(moodEntryIds).stream()
                .collect(Collectors.groupingBy(trigger -> trigger.getMoodEntry().getId()));

        PatientMentalHealthRecordDTO record = buildRecord(patient, doctor, profile, moodEntries, triggersByMoodEntryId);
        return renderPdf(record);
    }

    private PatientMentalHealthRecordDTO buildRecord(UserAccount patient,
                                                     UserAccount doctor,
                                                     UserProfileSnapshot profile,
                                                     List<MoodEntry> moodEntries,
                                                     Map<Long, List<EmotionalTrigger>> triggersByMoodEntryId) {

        List<PatientMentalHealthRecordDTO.MoodEntryRecordItem> entryItems = moodEntries.stream()
                .map(entry -> {
                    List<PatientMentalHealthRecordDTO.TriggerRecordItem> triggerItems = triggersByMoodEntryId
                            .getOrDefault(entry.getId(), List.of())
                            .stream()
                            .sorted(Comparator.comparing(EmotionalTrigger::getRecordedAt,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .map(trigger -> PatientMentalHealthRecordDTO.TriggerRecordItem.builder()
                                    .triggerType(trigger.getTriggerType())
                                    .description(trigger.getDescription())
                                    .intensity(trigger.getIntensity())
                                    .recordedAt(trigger.getRecordedAt())
                                    .build())
                            .toList();

                    return PatientMentalHealthRecordDTO.MoodEntryRecordItem.builder()
                            .moodEntryId(entry.getId())
                            .moodScore(entry.getMoodScore())
                            .moodDescription(entry.getMoodDescription())
                            .triggers(entry.getTriggers())
                            .createdAt(entry.getCreatedAt())
                            .clinicalTriggers(triggerItems)
                            .build();
                })
                .toList();

        int triggerCount = entryItems.stream()
                .mapToInt(item -> item.getClinicalTriggers() != null ? item.getClinicalTriggers().size() : 0)
                .sum();

        double avgMood = moodEntries.stream()
                .map(MoodEntry::getMoodScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return PatientMentalHealthRecordDTO.builder()
                .patientId(patient.getId())
                .patientFullName(buildDisplayName(patient))
                .patientEmail(patient.getEmail())
                .patientAvatarUrl(profile != null ? profile.getAvatar() : null)
                .patientBio(profile != null ? profile.getBio() : null)
                .preferredLanguage(profile != null ? profile.getPreferredLanguage() : null)
                .ageDisplay(buildAgeDisplay(patient.getDateOfBirth()))
                .doctorId(doctor.getId())
                .doctorFullName(buildDisplayName(doctor))
                .generatedAt(LocalDateTime.now())
                .totalMoodEntries(entryItems.size())
                .totalClinicalTriggers(triggerCount)
                .averageMoodScore(avgMood)
                .stabilitySummary(buildStabilitySummary(avgMood))
                .moodEntries(entryItems)
                .build();
    }

    private byte[] renderPdf(PatientMentalHealthRecordDTO record) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            document.add(new Paragraph("Patient Mental Health Record", titleFont));
            document.add(new Paragraph("Generated: " + formatDateTime(record.getGeneratedAt()), mutedFont));
            document.add(new Paragraph(" "));

            addProfileSection(document, record, sectionFont, bodyFont, mutedFont);
            addSummarySection(document, record, sectionFont, bodyFont);
            addMoodTimelineSection(document, record, sectionFont, bodyFont, mutedFont);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate patient record PDF", ex);
        }
    }

    private void addProfileSection(Document document,
                                   PatientMentalHealthRecordDTO record,
                                   Font sectionFont,
                                   Font bodyFont,
                                   Font mutedFont) throws DocumentException {
        document.add(new Paragraph("Patient CV Snapshot", sectionFont));

        PdfPTable profileTable = new PdfPTable(new float[]{1.2f, 2.8f});
        profileTable.setWidthPercentage(100);
        profileTable.setSpacingBefore(8);
        profileTable.setSpacingAfter(10);

        if (record.getPatientAvatarUrl() != null && !record.getPatientAvatarUrl().isBlank()) {
            try {
                Image image = Image.getInstance(new URL(record.getPatientAvatarUrl()));
                image.scaleToFit(80, 80);
                PdfPCell imageCell = new PdfPCell(image, false);
                imageCell.setPadding(6);
                profileTable.addCell(imageCell);
            } catch (Exception ignored) {
                profileTable.addCell(buildCell("Avatar unavailable", mutedFont));
            }
        } else {
            profileTable.addCell(buildCell("No avatar", mutedFont));
        }

        Paragraph info = new Paragraph();
        info.add(new Phrase("Name: " + safe(record.getPatientFullName()) + "\n", bodyFont));
        info.add(new Phrase("Email: " + safe(record.getPatientEmail()) + "\n", bodyFont));
        info.add(new Phrase("Age: " + safe(record.getAgeDisplay()) + "\n", bodyFont));
        info.add(new Phrase("Preferred Language: " + safe(record.getPreferredLanguage()) + "\n", bodyFont));
        info.add(new Phrase("Bio: " + safe(record.getPatientBio()) + "\n", bodyFont));
        info.add(new Phrase("Assigned Doctor: " + safe(record.getDoctorFullName()), bodyFont));

        PdfPCell infoCell = new PdfPCell(info);
        infoCell.setPadding(8);
        profileTable.addCell(infoCell);

        document.add(profileTable);
    }

    private void addSummarySection(Document document,
                                   PatientMentalHealthRecordDTO record,
                                   Font sectionFont,
                                   Font bodyFont) throws DocumentException {
        document.add(new Paragraph("Clinical Summary", sectionFont));
        document.add(new Paragraph("Total Mood Entries: " + record.getTotalMoodEntries(), bodyFont));
        document.add(new Paragraph("Total Clinical Triggers: " + record.getTotalClinicalTriggers(), bodyFont));
        document.add(new Paragraph("Average Mood Score: " + String.format(Locale.US, "%.2f", record.getAverageMoodScore()) + "/10", bodyFont));
        document.add(new Paragraph("Stability: " + safe(record.getStabilitySummary()), bodyFont));
        document.add(new Paragraph(" "));
    }

    private void addMoodTimelineSection(Document document,
                                        PatientMentalHealthRecordDTO record,
                                        Font sectionFont,
                                        Font bodyFont,
                                        Font mutedFont) throws DocumentException {
        document.add(new Paragraph("Mood Timeline and Clinical Triggers", sectionFont));
        document.add(new Paragraph(" "));

        for (PatientMentalHealthRecordDTO.MoodEntryRecordItem item : record.getMoodEntries()) {
            document.add(new Paragraph("Mood Entry #" + item.getMoodEntryId() + " (" + formatDateTime(item.getCreatedAt()) + ")", bodyFont));
            document.add(new Paragraph("Mood Score: " + item.getMoodScore() + "/10", bodyFont));
            document.add(new Paragraph("Description: " + safe(item.getMoodDescription()), bodyFont));
            document.add(new Paragraph("Patient Triggers: " + safe(item.getTriggers()), bodyFont));

            List<PatientMentalHealthRecordDTO.TriggerRecordItem> triggers = item.getClinicalTriggers();
            if (triggers == null || triggers.isEmpty()) {
                document.add(new Paragraph("Clinical Triggers: None", mutedFont));
            } else {
                PdfPTable triggerTable = new PdfPTable(new float[]{1.4f, 0.8f, 3.0f, 1.6f});
                triggerTable.setWidthPercentage(100);
                triggerTable.setSpacingBefore(6);
                triggerTable.setSpacingAfter(10);
                triggerTable.addCell(buildCell("Type", bodyFont));
                triggerTable.addCell(buildCell("Intensity", bodyFont));
                triggerTable.addCell(buildCell("Observation", bodyFont));
                triggerTable.addCell(buildCell("Recorded At", bodyFont));

                for (PatientMentalHealthRecordDTO.TriggerRecordItem trigger : triggers) {
                    triggerTable.addCell(buildCell(safe(trigger.getTriggerType()), mutedFont));
                    triggerTable.addCell(buildCell(String.valueOf(trigger.getIntensity()), mutedFont));
                    triggerTable.addCell(buildCell(safe(trigger.getDescription()), mutedFont));
                    triggerTable.addCell(buildCell(formatDateTime(trigger.getRecordedAt()), mutedFont));
                }

                document.add(triggerTable);
            }

            document.add(new Paragraph(" "));
        }
    }

    private PdfPCell buildCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        return cell;
    }

    private String buildDisplayName(UserAccount user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    private String buildStabilitySummary(double avgMood) {
        if (avgMood <= 3.5) {
            return "High instability (requires close follow-up)";
        }
        if (avgMood <= 5.5) {
            return "Moderate instability (monitor frequently)";
        }
        if (avgMood <= 7.5) {
            return "Generally stable with occasional low periods";
        }
        return "Stable mood trend";
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

    private String buildAgeDisplay(java.util.Date dateOfBirth) {
        if (dateOfBirth == null) {
            return "N/A";
        }

        try {
            LocalDate birthDate = java.time.Instant.ofEpochMilli(dateOfBirth.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            LocalDate today = LocalDate.now();

            if (birthDate.isAfter(today)) {
                return "N/A";
            }

            int years = Period.between(birthDate, today).getYears();
            return years + " years";
        } catch (RuntimeException ex) {
            return "N/A";
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatDateTime(java.util.Date date) {
        if (date == null) {
            return "N/A";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .format(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }
}

