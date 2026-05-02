package com.example.healthcare.dto;

import com.example.healthcare.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class DoctorResponseDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Date dateOfBirth;
    private Role role;
    private Date createdAt;
    private String specialty;
    private String profilePictureUrl;
    private UserProfileDTO profile;
    private Boolean isActive;
}
