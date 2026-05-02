package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.StockItemCreateRequestDTO;
import com.example.pharmacy.dto.StockItemResponseDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.StockState;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private MedicineStockItemRepository medicineStockItemRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks private StockServiceImpl service;

    @Test
    void listMyStock_passesNormalizedQuery() {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        when(medicineStockItemRepository.findByOwnerUserIdWithSearch(eq(7L), eq(false), eq("abc")))
                .thenReturn(List.of());

        List<StockItemResponseDTO> result = service.listMyStock("  abc ", false);
        assertThat(result).isEmpty();
    }

    @Test
    void createStockItem_whenNoPharmacy_throws() {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        when(pharmacyRepository.findByOwnerUserId(7L)).thenReturn(Optional.empty());

        StockItemCreateRequestDTO req = new StockItemCreateRequestDTO();
        req.setMedicineName("A");
        req.setQuantity(1);

        assertThatThrownBy(() -> service.createStockItem(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createStockItem_setsStateBasedOnQuantity() {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(1L);
        when(pharmacyRepository.findByOwnerUserId(7L)).thenReturn(Optional.of(pharmacy));

        StockItemCreateRequestDTO req = new StockItemCreateRequestDTO();
        req.setMedicineName("  Paracetamol ");
        req.setQuantity(0);
        req.setImageUrl("img");
        req.setDescription("d");

        MedicineStockItem saved = MedicineStockItem.builder()
                .id(99L)
                .pharmacy(pharmacy)
                .medicineName("Paracetamol")
                .quantity(0)
                .state(StockState.OUT_OF_STOCK)
                .archived(false)
                .build();
        when(medicineStockItemRepository.save(any(MedicineStockItem.class))).thenReturn(saved);

        StockItemResponseDTO resp = service.createStockItem(req);
        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getState()).isEqualTo(StockState.OUT_OF_STOCK);
    }

    @Test
    void renameStockItem_validatesName() {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        when(medicineStockItemRepository.findByIdAndPharmacyOwnerUserId(1L, 7L))
                .thenReturn(Optional.of(MedicineStockItem.builder()
                        .id(1L)
                        .pharmacy(Pharmacy.builder().id(1L).build())
                        .medicineName("Old")
                        .quantity(1)
                        .state(StockState.IN_STOCK)
                        .archived(false)
                        .build()));

        assertThatThrownBy(() -> service.renameStockItem(1L, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

