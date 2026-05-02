package com.example.pharmacy.dto;

import com.example.pharmacy.entity.PharmacyApplicationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPharmacyApplicationSummaryDTO {
    private Long id;
    private Long userId;
    private String applicantName;
    private String email;
    private String pharmacyName;
    private String city;
    private String governorate;
    private PharmacyApplicationStatus status;
    private String submittedAt;
    private String reviewedAt;
}
