package com.example.pharmacy.dto;

import com.example.pharmacy.entity.PharmacyApplicationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPharmacyApplicationDetailsDTO {
    private Long id;
    private Long userId;
    private PharmacyApplicationStatus status;
    private String submittedAt;
    private String reviewedAt;
    private Long reviewedByAdminId;
    private String reviewComment;

    private String firstName;
    private String lastName;
    private String email;
    private String cinNumber;
    private String cnopNumber;

    private String pharmacyName;
    private String authorizationReferenceNumber;
    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;

    private String cinDocumentUrl;
    private String cnoptProofUrl;
    private String legalDocumentUrl;

    private String cnoptMlDecision;
    private String cnoptMlFraudStatus;
    private String cnoptMlMessage;
    private Double cnoptMlRiskScore;
    private String cnoptMlCheckedAt;
}
