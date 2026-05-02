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
public class PortalResubmissionRequest {
    private String description;
    private Double amount;
    private Double reimbursementAmount;
    private Double insuranceGrade;
    private String message;
    private List<String> attachmentUrls;
}
