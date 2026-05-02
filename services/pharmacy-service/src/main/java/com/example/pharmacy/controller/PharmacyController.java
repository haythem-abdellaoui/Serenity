package com.example.pharmacy.controller;

import com.example.pharmacy.dto.PharmacyResponseDTO;
import com.example.pharmacy.dto.PharmacyUpsertRequestDTO;
import com.example.pharmacy.service.PharmacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PHARMACIST')")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @GetMapping
    public ResponseEntity<PharmacyResponseDTO> getMyPharmacy() {
        return ResponseEntity.ok(pharmacyService.getMyPharmacy());
    }

    @PostMapping
    public ResponseEntity<PharmacyResponseDTO> createOrUpdateMyPharmacy(@Valid @RequestBody PharmacyUpsertRequestDTO request) {
        return ResponseEntity.ok(pharmacyService.upsertMyPharmacy(request));
    }

    @PutMapping
    public ResponseEntity<PharmacyResponseDTO> updateMyPharmacy(@Valid @RequestBody PharmacyUpsertRequestDTO request) {
        return ResponseEntity.ok(pharmacyService.upsertMyPharmacy(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMyPharmacy() {
        pharmacyService.deleteMyPharmacy();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
