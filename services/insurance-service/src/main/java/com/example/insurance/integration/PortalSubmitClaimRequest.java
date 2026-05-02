package com.example.insurance.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortalSubmitClaimRequest {
    private String ref;
    private String patientName;
    private String description;
    private Double amount;
    private Double reimbursementAmount;
    private String insuranceCompany;
    private Double insuranceGrade;
    private List<String> attachmentUrls;
}

