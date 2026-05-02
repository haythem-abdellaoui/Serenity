package com.example.appointment.dto;

import com.example.appointment.entity.TeleconsultationStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeleconsultationResponse {

    private Long id;
    private String meetingUrl;
    private Integer durationMinutes;
    private Instant startTime;
    private Instant endTime;
    private String recordingUrl;
    private TeleconsultationStatus status;
}
