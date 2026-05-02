package com.example.marketplace.dto;

import com.example.marketplace.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponseDTO {
    private Long id;
    private String customerEmail;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private String shippingAddress;
    private String customerNote;
    private PaymentAttemptDTO paymentAttempt;
    private List<OrderItemResponseDTO> items;
    private LocalDateTime createdAt;
}
