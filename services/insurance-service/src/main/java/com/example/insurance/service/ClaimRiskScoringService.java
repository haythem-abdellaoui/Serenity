package com.example.insurance.service;

import com.example.insurance.dto.ClaimRiskScoreResponseDTO;

public interface ClaimRiskScoringService {
    ClaimRiskScoreResponseDTO scoreClaim(Long claimId);
}

