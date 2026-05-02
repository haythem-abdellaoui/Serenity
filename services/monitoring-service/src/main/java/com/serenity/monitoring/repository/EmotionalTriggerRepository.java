package com.serenity.monitoring.repository;

import com.serenity.monitoring.entity.EmotionalTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionalTriggerRepository extends JpaRepository<EmotionalTrigger, Long> {

    List<EmotionalTrigger> findByMoodEntryId(Long moodEntryId);

    List<EmotionalTrigger> findByDoctorId(Long doctorId);

    List<EmotionalTrigger> findByMoodEntryIdAndDoctorId(Long moodEntryId, Long doctorId);

    boolean existsByMoodEntryId(Long moodEntryId);

    List<EmotionalTrigger> findByMoodEntryIdIn(List<Long> moodEntryIds);

    @Query("SELECT t FROM EmotionalTrigger t JOIN FETCH t.moodEntry m WHERE m.id IN :moodEntryIds")
    List<EmotionalTrigger> findAllForExportByMoodEntryIds(@Param("moodEntryIds") List<Long> moodEntryIds);
}

