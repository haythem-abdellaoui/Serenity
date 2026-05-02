package com.example.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarAuthorizeUrlResponse {
    private String authorizeUrl;
    /** Echo of validated return path encoded into {@code state} for the SPA callback. */
    private String returnTo;
}
