package com.example.pharmacy.service;

import com.example.pharmacy.dto.PharmacyResponseDTO;
import com.example.pharmacy.dto.PharmacyUpsertRequestDTO;

public interface PharmacyService {
    PharmacyResponseDTO upsertMyPharmacy(PharmacyUpsertRequestDTO request);
    PharmacyResponseDTO getMyPharmacy();
    void deleteMyPharmacy();
}
