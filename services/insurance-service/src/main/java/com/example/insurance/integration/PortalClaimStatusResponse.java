package com.example.insurance.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortalClaimStatusResponse {
    private String ref;
    private String status;
    private Double reimbursementAmount;
    private String reason;
    private String infoRequestDeadline;
}

