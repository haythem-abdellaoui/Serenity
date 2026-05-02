package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PrescriptionLineResponseDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrescriptionResponseMapper {

    public PrescriptionResponseDTO toResponse(PharmacyPrescription workflow, List<PrescriptionLineResponseDTO> lines) {
        Pharmacy assignedPharmacy = workflow.getAssignedPharmacy();
        PrescriptionLineResponseDTO summaryLine = lines.isEmpty() ? null : lines.get(0);
        String updatedAt = workflow.getUpdatedAt() != null
            ? workflow.getUpdatedAt().toString()
            : (workflow.getCreatedAt() != null ? workflow.getCreatedAt().toString() : null);

        return PrescriptionResponseDTO.builder()
            .id(workflow.getId())
            .pharmacyId(assignedPharmacy != null ? assignedPharmacy.getId() : null)
            .pharmacyName(assignedPharmacy != null ? assignedPharmacy.getName() : null)
            .doctorId(workflow.getDoctorId())
            .patientId(workflow.getPatientId())
            .doctorName(workflow.getDoctorNameSnapshot())
            .patientName(workflow.getPatientNameSnapshot())
            .assignedToPharmacy(assignedPharmacy != null)
            .assignmentMessage(assignedPharmacy == null ? "Patient has no default pharmacy yet" : null)
            .medicationName(summaryLine != null ? summaryLine.getMedicationName() : null)
            .dosage(summaryLine != null ? summaryLine.getDosage() : null)
            .quantity(summaryLine != null ? summaryLine.getQuantity() : null)
            .instructions(summaryLine != null ? summaryLine.getInstructions() : null)
            .medicineLines(lines)
            .status(workflow.getStatus())
            .rejectionReason(workflow.getRejectionReason())
            .readyAt(workflow.getReadyAt() != null ? workflow.getReadyAt().toString() : null)
            .insuranceDocumentAvailable(workflow.getInsuranceDocumentPath() != null)
            .insuranceDocumentUploadedAt(
                workflow.getInsuranceDocumentUploadedAt() != null
                    ? workflow.getInsuranceDocumentUploadedAt().toString()
                    : null
            )
            .createdAt(workflow.getCreatedAt() != null ? workflow.getCreatedAt().toString() : null)
            .updatedAt(updatedAt)
            .build();
    }
}
