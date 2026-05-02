package com.example.pharmacy.service;

import com.example.pharmacy.dto.*;
import com.example.pharmacy.entity.PharmacyApplicationStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PharmacyApplicationService {

    PharmacyApplicationResponseDTO getMyApplication();

    PharmacyApplicationResponseDTO submitMyApplication(
        PharmacyApplicationSubmitRequestDTO request,
        MultipartFile cinDocument,
        MultipartFile cnoptProofDocument,
        MultipartFile legalProofDocument
    );

    List<AdminPharmacyApplicationSummaryDTO> listApplications(PharmacyApplicationStatus status);

    AdminPharmacyApplicationDetailsDTO getApplicationDetails(Long applicationId);

    AdminPharmacyApplicationDetailsDTO approveApplication(Long applicationId);

    AdminPharmacyApplicationDetailsDTO rejectApplication(Long applicationId, String reviewComment);

    PharmacyApplicationDocumentPayload getApplicationDocument(Long applicationId, String documentType);
}
