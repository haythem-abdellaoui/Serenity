package com.example.marketplace.controller;

import com.example.marketplace.dto.RecommendationResponseDTO;
import com.example.marketplace.dto.QuizRecommendationRequestDTO;
import com.example.marketplace.service.WellnessRecommendationEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/articles/recommendations", "/api/marketplace/recommendations"})
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final WellnessRecommendationEngine recommendationEngine;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecommendationResponseDTO> getMyRecommendations(
            Authentication authentication,
            @RequestHeader(value = "userId", required = false) Long userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access attempt to recommendations endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            String jwtToken = extractJwtToken(authorizationHeader);
            
            if (userEmail == null || jwtToken == null || userId == null) {
                log.warn("Missing user context for recommendations: email={}, tokenPresent={}, userId={}",
                        userEmail,
                        jwtToken != null,
                        userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            log.info("Fetching recommendations for user {} (id={})", userEmail, userId);
            RecommendationResponseDTO response = recommendationEngine.getPersonalizedRecommendations(userId, jwtToken);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/quiz")
    public ResponseEntity<RecommendationResponseDTO> getQuizRecommendations(
            @Valid @RequestBody QuizRecommendationRequestDTO request) {
        try {
            RecommendationResponseDTO response = recommendationEngine.getQuizBasedRecommendations(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating quiz recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String extractJwtToken(String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                return authorizationHeader.substring(7);
            }
        } catch (Exception e) {
            log.error("Error extracting JWT token: {}", e.getMessage());
        }
        return null;
    }
}

