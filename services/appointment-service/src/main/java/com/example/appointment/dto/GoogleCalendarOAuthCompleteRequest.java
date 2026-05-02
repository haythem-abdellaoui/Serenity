package com.example.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleCalendarOAuthCompleteRequest {

    @NotBlank
    private String code;
}
