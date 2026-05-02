package com.serenity.monitoring.mapper;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.entity.MoodEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MoodEntryMapperImpl implements MoodEntryMapper {

    @Override
    public MoodEntry toEntity(MoodEntryRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        MoodEntry entity = new MoodEntry();
        entity.setPatientId(dto.getPatientId());
        entity.setMoodScore(dto.getMoodScore());
        entity.setMoodDescription(dto.getMoodDescription());
        entity.setTriggers(dto.getTriggers());
        entity.setDoctorId(dto.getDoctorId());
        return entity;
    }

    @Override
    public MoodEntryResponseDTO toResponseDTO(MoodEntry entity) {
        if (entity == null) {
            return null;
        }

        return MoodEntryResponseDTO.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .moodScore(entity.getMoodScore())
                .moodDescription(entity.getMoodDescription())
                .triggers(entity.getTriggers())
                .doctorId(entity.getDoctorId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .aiRiskLevel(entity.getAiRiskLevel())
                .aiRiskConfidence(entity.getAiRiskConfidence())
                .aiRiskRecommendation(entity.getAiRiskRecommendation())
                .aiRiskType(entity.getAiRiskType())
                .aiMediumRiskType(entity.getAiMediumRiskType())
                .aiRiskScore(entity.getAiRiskScore())
                .build();
    }

    @Override
    public List<MoodEntryResponseDTO> toResponseDTOList(List<MoodEntry> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void updateEntityFromDTO(MoodEntryRequestDTO dto, MoodEntry entity) {
        if (dto == null) {
            return;
        }

        if (dto.getPatientId() != null) {
            entity.setPatientId(dto.getPatientId());
        }
        if (dto.getMoodScore() != null) {
            entity.setMoodScore(dto.getMoodScore());
        }
        if (dto.getMoodDescription() != null) {
            entity.setMoodDescription(dto.getMoodDescription());
        }
        if (dto.getTriggers() != null) {
            entity.setTriggers(dto.getTriggers());
        }
        if (dto.getDoctorId() != null) {
            entity.setDoctorId(dto.getDoctorId());
        }
    }
}
