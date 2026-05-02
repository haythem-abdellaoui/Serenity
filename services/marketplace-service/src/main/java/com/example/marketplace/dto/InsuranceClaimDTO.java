package com.example.marketplace.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceClaimDTO {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String status;
    private LocalDateTime approvalDate;
}
