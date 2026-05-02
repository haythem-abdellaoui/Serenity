package com.example.insurance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaimTransitionResponseDTO {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private Long changedByUserId;
    private String changedByRole;
    private String reason;
    private LocalDateTime changedAt;
}
