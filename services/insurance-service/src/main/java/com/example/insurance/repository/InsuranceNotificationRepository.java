package com.example.insurance.repository;

import com.example.insurance.entity.InsuranceNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceNotificationRepository extends JpaRepository<InsuranceNotification, Long> {
    List<InsuranceNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
}

