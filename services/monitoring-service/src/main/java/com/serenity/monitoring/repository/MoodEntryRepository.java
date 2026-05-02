package com.serenity.monitoring.repository;

import com.serenity.monitoring.entity.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodEntryRepository extends JpaRepository<MoodEntry, Long> {

    /**
     * Find all mood entries for a specific patient
     */
    List<MoodEntry> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    /**
     * Find all mood entries assigned to a specific doctor.
     */
    List<MoodEntry> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    /**
     * Find all mood entries for one doctor-patient relationship.
     */
    List<MoodEntry> findByDoctorIdAndPatientIdOrderByCreatedAtDesc(Long doctorId, Long patientId);

    /**
     * Find mood entries by patient ID and date range
     */
    @Query("SELECT m FROM MoodEntry m WHERE m.patientId = :patientId AND m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<MoodEntry> findByPatientIdAndDateRange(@Param("patientId") Long patientId,
                                                 @Param("startDate") java.util.Date startDate,
                                                 @Param("endDate") java.util.Date endDate);

    /**
     * Check if a mood entry belongs to a patient
     */
    boolean existsByIdAndPatientId(Long id, Long patientId);

    /**
     * Find the first mood entry for a patient (to get their assigned doctor)
     */
    Optional<MoodEntry> findFirstByPatientIdOrderByCreatedAtAsc(Long patientId);

    /**
     * Count total mood entries assigned to a doctor.
     */
    long countByDoctorId(Long doctorId);

    /**
     * Count distinct patients assigned to a doctor.
     * This is the primary metric for fair doctor load balancing.
     */
    @Query("SELECT COUNT(DISTINCT m.patientId) FROM MoodEntry m WHERE m.doctorId = :doctorId")
    long countDistinctPatientsByDoctorId(@Param("doctorId") Long doctorId);

    @Query("""
            SELECT COUNT(m) FROM MoodEntry m
            WHERE m.createdAt < :cutoff
              AND NOT EXISTS (
                SELECT t.id FROM EmotionalTrigger t WHERE t.moodEntry.id = m.id
              )
            """)
    long countOldEntriesWithoutClinicalTriggers(@Param("cutoff") Date cutoff);

    @Modifying
    @Query("""
            DELETE FROM MoodEntry m
            WHERE m.createdAt < :cutoff
              AND NOT EXISTS (
                SELECT t.id FROM EmotionalTrigger t WHERE t.moodEntry.id = m.id
              )
            """)
    int deleteOldEntriesWithoutClinicalTriggers(@Param("cutoff") Date cutoff);
}
