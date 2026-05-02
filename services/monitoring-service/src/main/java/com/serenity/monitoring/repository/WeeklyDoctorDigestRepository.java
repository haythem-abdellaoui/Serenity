package com.serenity.monitoring.repository;

import com.serenity.monitoring.entity.WeeklyDoctorDigest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyDoctorDigestRepository extends JpaRepository<WeeklyDoctorDigest, Long> {

    boolean existsByDoctorIdAndWeekStartDate(Long doctorId, LocalDate weekStartDate);

    Optional<WeeklyDoctorDigest> findTopByDoctorIdOrderByWeekStartDateDesc(Long doctorId);

    List<WeeklyDoctorDigest> findTop12ByDoctorIdOrderByWeekStartDateDesc(Long doctorId);
}

