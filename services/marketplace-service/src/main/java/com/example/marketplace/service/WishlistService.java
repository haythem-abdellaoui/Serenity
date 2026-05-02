package com.example.marketplace.service;

import com.example.marketplace.dto.WishlistItemDTO;

import java.util.List;

public interface WishlistService {
    WishlistItemDTO addToWishlist(Long userId, Long productId);
    void removeFromWishlist(Long userId, Long productId);
    List<WishlistItemDTO> getUserWishlist(Long userId);
    boolean isProductInWishlist(Long userId, Long productId);
}
