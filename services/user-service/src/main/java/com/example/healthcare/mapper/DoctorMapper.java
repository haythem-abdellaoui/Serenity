package com.example.healthcare.mapper;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.dto.UserProfileDTO;
import com.example.healthcare.entity.Doctor;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public DoctorResponseDTO toDTO(Doctor doctor) {
        DoctorResponseDTO dto = DoctorResponseDTO.builder()
                .id(doctor.getId())
                .email(doctor.getEmail())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .phone(doctor.getPhone())
                .dateOfBirth(doctor.getDateOfBirth())
                .role(doctor.getRole())
                .createdAt(doctor.getCreatedAt())
                .specialty(doctor.getSpecialty())
                .profilePictureUrl(doctor.getProfilePictureUrl())
                .isActive(doctor.getIsActive())
                .build();

        if (doctor.getProfile() != null) {
            dto.setProfile(UserProfileDTO.builder()
                    .id(doctor.getProfile().getId())
                    .avatar(doctor.getProfile().getAvatar())
                    .bio(doctor.getProfile().getBio())
                    .isAnonymous(doctor.getProfile().getIsAnonymous())
                    .preferredLanguage(doctor.getProfile().getPreferredLanguage())
                    .build());
        }

        return dto;
    }
}