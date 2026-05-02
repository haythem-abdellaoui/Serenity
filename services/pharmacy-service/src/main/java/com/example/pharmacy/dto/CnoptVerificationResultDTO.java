package com.example.pharmacy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CnoptVerificationResultDTO {
    private String documentDecision;
    private String fraudStatus;
    private String message;
    private Boolean isCnoptDocument;
    private Double fraudRiskScore;
    private List<String> flags;
}
