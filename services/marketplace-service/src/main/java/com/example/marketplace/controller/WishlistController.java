package com.example.marketplace.controller;

import com.example.marketplace.dto.WishlistItemDTO;
import com.example.marketplace.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/articles/wishlist", "/api/marketplace/wishlist"})
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WishlistItemDTO> addToWishlist(
            @RequestHeader("userId") Long userId,
            @PathVariable Long productId) {
        WishlistItemDTO wishlistItem = wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistItem);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromWishlist(
            @RequestHeader("userId") Long userId,
            @PathVariable Long productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WishlistItemDTO>> getUserWishlist(@RequestHeader("userId") Long userId) {
        List<WishlistItemDTO> wishlist = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isInWishlist(
            @RequestHeader("userId") Long userId,
            @PathVariable Long productId) {
        boolean inWishlist = wishlistService.isProductInWishlist(userId, productId);
        return ResponseEntity.ok(inWishlist);
    }
}
