package com.example.marketplace.repository;

import com.example.marketplace.entity.MarketplaceOrder;
import com.example.marketplace.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    List<MarketplaceOrder> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    List<MarketplaceOrder> findAllByOrderByCreatedAtDesc();

    List<MarketplaceOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<MarketplaceOrder> findByCustomerUserIdAndStatusOrderByCreatedAtDesc(Long customerUserId, OrderStatus status);
}
