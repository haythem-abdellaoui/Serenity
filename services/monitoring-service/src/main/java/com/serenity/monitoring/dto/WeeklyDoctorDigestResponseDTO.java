package com.serenity.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyDoctorDigestResponseDTO {

    private Long id;
    private Long doctorId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Integer crisisCount;
    private Integer worseningPatients;
    private Integer noCheckinPatients;
    private String summaryMessage;
    private Date generatedAt;
}

