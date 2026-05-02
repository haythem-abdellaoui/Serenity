package com.example.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarStatusResponse {
    /** False when server has no OAuth client configured. */
    private boolean configured;
    private boolean connected;
    private String googleEmail;
}
