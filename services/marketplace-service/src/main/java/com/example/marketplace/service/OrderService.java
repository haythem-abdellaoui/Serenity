package com.example.marketplace.service;

import com.example.marketplace.dto.CheckoutRequestDTO;
import com.example.marketplace.dto.OrderResponseDTO;
import com.example.marketplace.dto.OrderStatusUpdateRequestDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO checkout(String customerEmail, Long userId, CheckoutRequestDTO request);

    List<OrderResponseDTO> getMyOrders(String customerEmail);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO getOrderById(Long orderId);

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatusUpdateRequestDTO request);

    void cancelOrder(Long orderId);
}
