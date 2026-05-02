package com.example.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarSyncResponse {
    /** How many events were created or updated in Google Calendar. */
    private int eventsUpserted;
    /** PENDING/CONFIRMED slots considered (may be 0 if you have no eligible visits). */
    private int totalCandidates;
}
