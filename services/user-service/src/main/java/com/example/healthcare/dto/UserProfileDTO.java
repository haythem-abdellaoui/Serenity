package com.example.healthcare.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDTO {
    private Long id;
    private String avatar;
    private String bio;
    private Boolean isAnonymous;
    private String preferredLanguage;
}
