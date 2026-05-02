package com.example.insurance.service;

import com.example.insurance.dto.InsuranceNotificationResponseDTO;
import com.example.insurance.entity.NotificationType;

import java.util.List;

public interface NotificationService {
    void createNotification(Long userId, Long claimId, NotificationType type, String title, String message);
    List<InsuranceNotificationResponseDTO> getUserNotifications(Long userId);
    List<InsuranceNotificationResponseDTO> getAllNotifications();
    long countUnread(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
}

