package com.serenity.monitoring.mapper;

import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;
import com.serenity.monitoring.entity.EmotionalTrigger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmotionalTriggerMapper {

    @Mapping(source = "moodEntry.id", target = "moodEntryId")
    EmotionalTriggerResponse toResponse(EmotionalTrigger trigger);

    @Mapping(source = "moodEntryId", target = "moodEntry.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctorId", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    EmotionalTrigger toEntity(EmotionalTriggerRequest request);
}

