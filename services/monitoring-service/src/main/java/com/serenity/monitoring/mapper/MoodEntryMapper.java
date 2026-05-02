package com.serenity.monitoring.mapper;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.entity.MoodEntry;

import java.util.List;

public interface MoodEntryMapper {

    MoodEntry toEntity(MoodEntryRequestDTO dto);

    MoodEntryResponseDTO toResponseDTO(MoodEntry entity);

    List<MoodEntryResponseDTO> toResponseDTOList(List<MoodEntry> entities);

    void updateEntityFromDTO(MoodEntryRequestDTO dto, MoodEntry entity);
}
