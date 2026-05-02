package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.DoctorDTO;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.service.DoctorAssignmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DoctorAssignmentServiceImpl implements DoctorAssignmentService {

    private final MoodEntryRepository moodEntryRepository;
    private final UserAccountRepository userAccountRepository;

    public DoctorAssignmentServiceImpl(MoodEntryRepository moodEntryRepository,
                                       UserAccountRepository userAccountRepository) {
        this.moodEntryRepository = moodEntryRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Get the assigned doctor for a patient
     * If patient has no assigned doctor, assign one using Round-Robin algorithm
     */
    @Override
    public Long getOrAssignDoctor(Long patientId) {
        // Check if patient already has a mood entry (which means they have an assigned doctor)
        var existingEntry = moodEntryRepository.findFirstByPatientIdOrderByCreatedAtAsc(patientId);
        
        if (existingEntry.isPresent() && existingEntry.get().getDoctorId() != null) {
            log.info("Patient {} already assigned to doctor {}", patientId, existingEntry.get().getDoctorId());
            return existingEntry.get().getDoctorId();
        }
        
        // No existing mood entry, assign a new doctor using Round-Robin
        log.info("Assigning new doctor to patient {}", patientId);
        return assignDoctorToPatient(patientId);
    }

    /**
     * Assign a doctor to a patient using least-load balancing.
     * The load metric is the number of DISTINCT patients already assigned per doctor.
     */
    @Override
    public Long assignDoctorToPatient(Long patientId) {
        List<DoctorDTO> doctors = getAllDoctors();
        
        if (doctors.isEmpty()) {
            throw new IllegalStateException("No doctors available in the system");
        }
        
        log.info("Found {} doctors in system", doctors.size());
        
        // Find doctor with the least assigned distinct patients
        Long assignedDoctorId = doctors.get(0).getId();
        long minPatientCount = moodEntryRepository.countDistinctPatientsByDoctorId(doctors.get(0).getId());
        
        for (DoctorDTO doctor : doctors) {
            long patientCount = moodEntryRepository.countDistinctPatientsByDoctorId(doctor.getId());
            log.debug("Doctor {} currently has {} distinct patients assigned", doctor.getId(), patientCount);
            
            if (patientCount < minPatientCount) {
                minPatientCount = patientCount;
                assignedDoctorId = doctor.getId();
            }
        }
        
        log.info("Patient {} assigned to doctor {} (with {} distinct patients)", 
                 patientId, assignedDoctorId, minPatientCount);
        
        return assignedDoctorId;
    }

    /**
     * Get all active doctors from users table.
     */
    @Override
    public List<DoctorDTO> getAllDoctors() {
        List<UserAccount> doctors = userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR");
        if (doctors.isEmpty()) {
            throw new IllegalStateException("No active doctors found in the system");
        }

        List<DoctorDTO> result = doctors.stream()
                .map(user -> DoctorDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .toList();

        log.debug("Retrieved {} doctors from local users table", result.size());
        return result;
    }
}
