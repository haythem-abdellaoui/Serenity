package com.example.insurance.entity;

public enum ClaimStatus {
    // Legacy status kept for backward compatibility with existing rows.
    PENDING,
    SUBMITTED,
    UNDER_REVIEW,
    NEEDS_INFO,
    APPROVED,
    PARTIALLY_APPROVED,
    PAID,
    REJECTED
}
