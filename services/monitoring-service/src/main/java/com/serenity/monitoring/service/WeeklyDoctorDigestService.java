package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.WeeklyDoctorDigestResponseDTO;

import java.util.List;

public interface WeeklyDoctorDigestService {

    void generateWeeklyDigests();

    WeeklyDoctorDigestResponseDTO getLatestDigestForDoctor(Long doctorId);

    List<WeeklyDoctorDigestResponseDTO> getRecentDigestsForDoctor(Long doctorId);
}

