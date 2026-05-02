package com.example.pharmacy.controller;

import com.example.pharmacy.dto.PharmacyApplicationResponseDTO;
import com.example.pharmacy.dto.PharmacyApplicationSubmitRequestDTO;
import com.example.pharmacy.service.PharmacyApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pharmacy/applications")
@RequiredArgsConstructor
public class PharmacyApplicationController {

    private final PharmacyApplicationService pharmacyApplicationService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PATIENT','PHARMACIST')")
    public ResponseEntity<PharmacyApplicationResponseDTO> getMyApplication() {
        return ResponseEntity.ok(pharmacyApplicationService.getMyApplication());
    }

    @PostMapping(path = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PharmacyApplicationResponseDTO> submitMyApplication(
        @Valid @RequestPart("application") PharmacyApplicationSubmitRequestDTO request,
        @RequestPart(value = "cinDocument", required = false) MultipartFile cinDocument,
        @RequestPart(value = "cnoptProofDocument", required = false) MultipartFile cnoptProofDocument,
        @RequestPart(value = "legalProofDocument", required = false) MultipartFile legalProofDocument
    ) {
        return ResponseEntity.ok(pharmacyApplicationService.submitMyApplication(
            request,
            cinDocument,
            cnoptProofDocument,
            legalProofDocument
        ));
    }
}
