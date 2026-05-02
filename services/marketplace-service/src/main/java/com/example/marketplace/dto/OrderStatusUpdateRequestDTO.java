package com.example.marketplace.dto;

import com.example.marketplace.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequestDTO {

    @NotNull
    private OrderStatus status;
}
