package com.example.insurance.service.impl;

import com.example.insurance.dto.InsuranceNotificationResponseDTO;
import com.example.insurance.entity.InsuranceNotification;
import com.example.insurance.entity.NotificationType;
import com.example.insurance.repository.InsuranceNotificationRepository;
import com.example.insurance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final InsuranceNotificationRepository notificationRepository;

    @Override
    public void createNotification(Long userId, Long claimId, NotificationType type, String title, String message) {
        InsuranceNotification notification = InsuranceNotification.builder()
                .userId(userId)
                .claimId(claimId)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceNotificationResponseDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceNotificationResponseDTO> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        InsuranceNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot mark another user's notification as read.");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<InsuranceNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (InsuranceNotification notification : notifications) {
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    private InsuranceNotificationResponseDTO toDto(InsuranceNotification n) {
        return InsuranceNotificationResponseDTO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .claimId(n.getClaimId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(Boolean.TRUE.equals(n.getIsRead()))
                .createdAt(n.getCreatedAt())
                .build();
    }
}

