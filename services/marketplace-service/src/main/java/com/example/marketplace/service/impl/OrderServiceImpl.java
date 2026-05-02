package com.example.marketplace.service.impl;

import com.example.marketplace.dto.*;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.MarketplaceOrderRepository;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final MarketplaceOrderRepository marketplaceOrderRepository;

    @Override
    @Transactional
    public OrderResponseDTO checkout(String customerEmail, Long userId, CheckoutRequestDTO request) {
                Optional<OrderResponseDTO> existingUnlock = findExistingUnlockOrder(userId, request);
                if (existingUnlock.isPresent()) {
                        return existingUnlock.get();
                }

        MarketplaceOrder order = MarketplaceOrder.builder()
                .customerEmail(customerEmail)
                .customerUserId(userId)
                .shippingAddress(request.getShippingAddress().trim())
                .customerNote(request.getCustomerNote())
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.MOCK_AUTHORIZED)
                .paymentReference("MOCK-" + UUID.randomUUID())
                .totalAmount(BigDecimal.ZERO)
                .currency("TND")
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CheckoutItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + item.getProductId()));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException("Product is not available: " + product.getName());
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);

            MarketplaceOrderItem orderItem = MarketplaceOrderItem.builder()
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(product.getPrice())
                    .lineTotal(lineTotal)
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PAID);

        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        return toResponse(saved);
    }

        private Optional<OrderResponseDTO> findExistingUnlockOrder(Long userId, CheckoutRequestDTO request) {
                if (userId == null || request.getItems() == null || request.getItems().size() != 1) {
                        return Optional.empty();
                }

                CheckoutItemDTO checkoutItem = request.getItems().get(0);
                if (checkoutItem == null || checkoutItem.getProductId() == null) {
                        return Optional.empty();
                }

                Long productId = checkoutItem.getProductId();
                Product product = productRepository.findById(productId).orElse(null);
                if (product == null || product.getType() != ProductType.DIGITAL || !Boolean.TRUE.equals(product.getPreviewable())) {
                        return Optional.empty();
                }

                return marketplaceOrderRepository.findByCustomerUserIdAndStatusOrderByCreatedAtDesc(userId, OrderStatus.PAID)
                                .stream()
                                .filter(order -> order.getItems().stream().anyMatch(i -> i.getProduct().getId().equals(productId)))
                                .findFirst()
                                .map(this::toResponse);
        }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(String customerEmail) {
        return marketplaceOrderRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponseDTO> getAllOrders() {
                return marketplaceOrderRepository.findAllByOrderByCreatedAtDesc()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public OrderResponseDTO getOrderById(Long orderId) {
                MarketplaceOrder order = findOrderById(orderId);
                return toResponse(order);
        }

        @Override
        @Transactional
        public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatusUpdateRequestDTO request) {
                MarketplaceOrder order = findOrderById(orderId);
                validateStatusTransition(order.getStatus(), request.getStatus());
                order.setStatus(request.getStatus());
                return toResponse(marketplaceOrderRepository.save(order));
        }

        @Override
        @Transactional
        public void cancelOrder(Long orderId) {
                MarketplaceOrder order = findOrderById(orderId);
                if (order.getStatus() == OrderStatus.CANCELLED) {
                        return;
                }
                order.setStatus(OrderStatus.CANCELLED);
                marketplaceOrderRepository.save(order);
        }

        private MarketplaceOrder findOrderById(Long orderId) {
                return marketplaceOrderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id=" + orderId));
        }

        private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
                if (currentStatus == newStatus) {
                        return;
                }

                if (currentStatus == OrderStatus.CANCELLED) {
                        throw new IllegalArgumentException("Cancelled orders cannot be changed");
                }

                if (currentStatus == OrderStatus.PAID && newStatus == OrderStatus.CREATED) {
                        throw new IllegalArgumentException("Cannot move PAID orders back to CREATED");
                }
        }

    private OrderResponseDTO toResponse(MarketplaceOrder order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> OrderItemResponseDTO.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        PaymentAttemptDTO payment = PaymentAttemptDTO.builder()
                .reference(order.getPaymentReference())
                .status(order.getPaymentStatus())
                .message(order.getPaymentStatus() == PaymentStatus.MOCK_AUTHORIZED
                        ? "Mock payment authorized"
                        : "Mock payment declined")
                .build();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .customerNote(order.getCustomerNote())
                .paymentAttempt(payment)
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
