package com.example.marketplace.service;

import com.example.marketplace.dto.ProductReviewDTO;

import java.util.List;

public interface ReviewService {
    ProductReviewDTO createOrUpdateReview(Long userId, String userEmail, Long productId, Integer rating, String reviewText);
    ProductReviewDTO getReviewById(Long reviewId);
    List<ProductReviewDTO> getProductReviews(Long productId);
    List<ProductReviewDTO> getUserReviews(Long userId);
    void deleteReview(Long reviewId, Long userId);
    Double getAverageRating(Long productId);
}
