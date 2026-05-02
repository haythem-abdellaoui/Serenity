package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.DoctorDTO;

import java.util.List;

public interface DoctorAssignmentService {

    /**
     * Get the assigned doctor for a patient.
     * If patient has no assigned doctor, assign one using balancing algorithm.
     */
    Long getOrAssignDoctor(Long patientId);

    /**
     * Get all doctors available for assignment.
     */
    List<DoctorDTO> getAllDoctors();

    /**
     * Assign a doctor to a patient.
     */
    Long assignDoctorToPatient(Long patientId);
}

