package com.example.healthcare.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorUpdateRequest {

    // User fields
    private String firstName;
    private String lastName;
    private String phone;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date dateOfBirth;

    // UserProfile fields
    private String avatarUrl;
    private String bio;
    private String preferredLanguage;
    private Boolean isAnonymous;

    // Doctor fields
    private String specialty;
    private MultipartFile image; // profile picture file upload
}