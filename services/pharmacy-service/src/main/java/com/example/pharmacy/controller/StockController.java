package com.example.pharmacy.controller;

import com.example.pharmacy.dto.StockItemCreateRequestDTO;
import com.example.pharmacy.dto.StockItemRenameRequestDTO;
import com.example.pharmacy.dto.StockItemResponseDTO;
import com.example.pharmacy.dto.StockQuantityIncrementRequestDTO;
import com.example.pharmacy.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/stock")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PHARMACIST')")
@Validated
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<StockItemResponseDTO>> listStock(
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(stockService.listMyStock(query, includeArchived));
    }

    @PostMapping
    public ResponseEntity<StockItemResponseDTO> createStockItem(@Valid @RequestBody StockItemCreateRequestDTO request) {
        return ResponseEntity.ok(stockService.createStockItem(request));
    }

    @PatchMapping("/{stockItemId}/rename")
    public ResponseEntity<StockItemResponseDTO> renameStockItem(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId,
        @Valid @RequestBody StockItemRenameRequestDTO request
    ) {
        return ResponseEntity.ok(stockService.renameStockItem(stockItemId, request.getMedicineName()));
    }

    @PatchMapping("/{stockItemId}/increment")
    public ResponseEntity<StockItemResponseDTO> incrementQuantity(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId,
        @Valid @RequestBody StockQuantityIncrementRequestDTO request
    ) {
        return ResponseEntity.ok(stockService.incrementQuantity(stockItemId, request.getIncrementBy()));
    }

    @PatchMapping("/{stockItemId}/out-of-stock")
    public ResponseEntity<StockItemResponseDTO> markOutOfStock(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId
    ) {
        return handleMarkOutOfStock(stockItemId);
    }

    @PostMapping("/{stockItemId}/out-of-stock")
    // Backward-compatible alias for clients still using POST instead of PATCH.
    public ResponseEntity<StockItemResponseDTO> markOutOfStockPost(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId
    ) {
        return handleMarkOutOfStock(stockItemId);
    }

    @DeleteMapping("/{stockItemId}")
    public ResponseEntity<Void> archiveStockItem(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId
    ) {
        stockService.archiveStockItem(stockItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{stockItemId}/permanent")
    public ResponseEntity<Void> deleteArchivedStockItem(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId
    ) {
        stockService.deleteArchivedStockItem(stockItemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{stockItemId}/restore")
    public ResponseEntity<StockItemResponseDTO> restoreStockItem(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId
    ) {
        return handleRestoreStockItem(stockItemId);
    }

    @PostMapping("/{stockItemId}/restore")
    // Backward-compatible alias for clients still using POST instead of PATCH.
    public ResponseEntity<StockItemResponseDTO> restoreStockItemPost(
        @PathVariable @Positive(message = "Stock item id must be positive") Long stockItemId
    ) {
        return handleRestoreStockItem(stockItemId);
    }

    private ResponseEntity<StockItemResponseDTO> handleMarkOutOfStock(Long stockItemId) {
        return ResponseEntity.ok(stockService.markOutOfStock(stockItemId));
    }

    private ResponseEntity<StockItemResponseDTO> handleRestoreStockItem(Long stockItemId) {
        return ResponseEntity.ok(stockService.restoreStockItem(stockItemId));
    }
}
