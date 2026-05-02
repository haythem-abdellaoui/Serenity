package com.example.marketplace.controller;

import com.example.marketplace.dto.CreateReviewRequestDTO;
import com.example.marketplace.dto.ProductReviewDTO;
import com.example.marketplace.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/articles/reviews", "/api/marketplace/reviews"})
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
        @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductReviewDTO> createOrUpdateReview(
            @RequestHeader("userId") Long userId,
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequestDTO request) {
        ProductReviewDTO review = reviewService.createOrUpdateReview(
                userId,
            authentication.getName(),
                request.getProductId(),
                request.getRating(),
                request.getReviewText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductReviewDTO>> getProductReviews(@PathVariable Long productId) {
        List<ProductReviewDTO> reviews = reviewService.getProductReviews(productId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/product/{productId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long productId) {
        Double averageRating = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(averageRating);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductReviewDTO>> getUserReviews(@RequestHeader("userId") Long userId) {
        List<ProductReviewDTO> reviews = reviewService.getUserReviews(userId);
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader("userId") Long userId) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ProductReviewDTO> getReview(@PathVariable Long reviewId) {
        ProductReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }
}