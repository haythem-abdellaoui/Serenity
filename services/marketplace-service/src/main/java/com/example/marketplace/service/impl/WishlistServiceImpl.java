package com.example.marketplace.service.impl;

import com.example.marketplace.dto.WishlistItemDTO;
import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.Wishlist;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.repository.WishlistRepository;
import com.example.marketplace.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Override
    public WishlistItemDTO addToWishlist(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        boolean exists = wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent();
        if (exists) {
            throw new IllegalArgumentException("Product already in wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .userId(userId)
                .product(product)
                .build();

        wishlist = wishlistRepository.save(wishlist);
        return mapToDTO(wishlist);
    }

    @Override
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInWishlist(Long userId, Long productId) {
        return wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    private WishlistItemDTO mapToDTO(Wishlist wishlist) {
        return WishlistItemDTO.builder()
                .id(wishlist.getId())
                .productId(wishlist.getProduct().getId())
                .productName(wishlist.getProduct().getName())
                .productPrice(wishlist.getProduct().getPrice())
                .productImageUrl(wishlist.getProduct().getImageUrl())
            .productType(wishlist.getProduct().getType())
            .productPreviewable(wishlist.getProduct().getPreviewable())
                .addedAt(wishlist.getAddedAt())
                .build();
    }
}
