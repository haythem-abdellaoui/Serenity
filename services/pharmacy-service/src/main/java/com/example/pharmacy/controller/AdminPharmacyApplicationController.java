package com.example.pharmacy.controller;

import com.example.pharmacy.dto.AdminPharmacyApplicationDetailsDTO;
import com.example.pharmacy.dto.AdminPharmacyApplicationSummaryDTO;
import com.example.pharmacy.dto.PharmacyApplicationRejectRequestDTO;
import com.example.pharmacy.entity.PharmacyApplicationStatus;
import com.example.pharmacy.service.PharmacyApplicationDocumentPayload;
import com.example.pharmacy.service.PharmacyApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/admin/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminPharmacyApplicationController {

    private final PharmacyApplicationService pharmacyApplicationService;

    @GetMapping
    public ResponseEntity<List<AdminPharmacyApplicationSummaryDTO>> listApplications(
        @RequestParam(required = false) PharmacyApplicationStatus status
    ) {
        return ResponseEntity.ok(pharmacyApplicationService.listApplications(status));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<AdminPharmacyApplicationDetailsDTO> getApplicationDetails(
        @PathVariable @Positive(message = "Application id must be positive") Long applicationId
    ) {
        return ResponseEntity.ok(pharmacyApplicationService.getApplicationDetails(applicationId));
    }

    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<AdminPharmacyApplicationDetailsDTO> approveApplication(
        @PathVariable @Positive(message = "Application id must be positive") Long applicationId
    ) {
        return ResponseEntity.ok(pharmacyApplicationService.approveApplication(applicationId));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<AdminPharmacyApplicationDetailsDTO> rejectApplication(
        @PathVariable @Positive(message = "Application id must be positive") Long applicationId,
        @Valid @RequestBody PharmacyApplicationRejectRequestDTO request
    ) {
        return ResponseEntity.ok(
            pharmacyApplicationService.rejectApplication(applicationId, request.getReviewComment())
        );
    }

    @GetMapping("/{applicationId}/documents/{documentType}")
    public ResponseEntity<Resource> getApplicationDocument(
        @PathVariable @Positive(message = "Application id must be positive") Long applicationId,
        @PathVariable @NotBlank(message = "Document type is required") String documentType
    ) {
        PharmacyApplicationDocumentPayload payload = pharmacyApplicationService
            .getApplicationDocument(applicationId, documentType);

        MediaType mediaType = MediaType.parseMediaType(payload.contentType());
        ContentDisposition disposition = ContentDisposition.inline().filename(payload.fileName()).build();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(payload.resource());
    }
}
