package com.example.marketplace.dto;

import com.example.marketplace.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentAttemptDTO {
    private String reference;
    private PaymentStatus status;
    private String message;
}
