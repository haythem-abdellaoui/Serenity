package com.example.insurance.service.impl;

import com.example.insurance.dto.InsuranceNotificationResponseDTO;
import com.example.insurance.entity.InsuranceNotification;
import com.example.insurance.entity.NotificationType;
import com.example.insurance.repository.InsuranceNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private InsuranceNotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl service;

    @Captor
    private ArgumentCaptor<InsuranceNotification> notificationCaptor;

    @Test
    void createNotification_shouldSaveEntityWithUnreadFlag() {
        service.createNotification(10L, 20L, NotificationType.CLAIM_APPROVED, "t", "m");

        verify(notificationRepository).save(notificationCaptor.capture());
        InsuranceNotification saved = notificationCaptor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals(20L, saved.getClaimId());
        assertEquals(NotificationType.CLAIM_APPROVED, saved.getType());
        assertEquals("t", saved.getTitle());
        assertEquals("m", saved.getMessage());
        assertEquals(false, saved.getIsRead());
    }

    @Test
    void getUserNotifications_shouldMapToDto() {
        InsuranceNotification n = InsuranceNotification.builder()
                .id(1L)
                .userId(10L)
                .claimId(20L)
                .type(NotificationType.CLAIM_REJECTED)
                .title("title")
                .message("message")
                .isRead(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(n));

        List<InsuranceNotificationResponseDTO> dtos = service.getUserNotifications(10L);

        assertEquals(1, dtos.size());
        InsuranceNotificationResponseDTO dto = dtos.get(0);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getUserId());
        assertEquals(20L, dto.getClaimId());
        assertEquals(NotificationType.CLAIM_REJECTED.name(), dto.getType());
        assertEquals("title", dto.getTitle());
        assertEquals("message", dto.getMessage());
        assertEquals(true, dto.isRead());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void getAllNotifications_shouldSortByCreatedAtDesc() {
        InsuranceNotification older = InsuranceNotification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.CLAIM_APPROVED)
                .title("a")
                .message("a")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
        InsuranceNotification newer = InsuranceNotification.builder()
                .id(2L)
                .userId(10L)
                .type(NotificationType.CLAIM_APPROVED)
                .title("b")
                .message("b")
                .createdAt(LocalDateTime.of(2026, 2, 1, 10, 0))
                .build();
        when(notificationRepository.findAll()).thenReturn(List.of(older, newer));

        List<InsuranceNotificationResponseDTO> dtos = service.getAllNotifications();

        assertEquals(2, dtos.size());
        assertEquals(2L, dtos.get(0).getId());
        assertEquals(1L, dtos.get(1).getId());
    }

    @Test
    void countUnread_shouldDelegateToRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(10L)).thenReturn(7L);
        assertEquals(7L, service.countUnread(10L));
    }

    @Test
    void markAsRead_shouldThrowWhenNotOwner() {
        InsuranceNotification n = InsuranceNotification.builder()
                .id(1L)
                .userId(99L)
                .type(NotificationType.CLAIM_APPROVED)
                .title("t")
                .message("m")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(RuntimeException.class, () -> service.markAsRead(1L, 10L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_shouldSetReadAndSaveWhenOwner() {
        InsuranceNotification n = InsuranceNotification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.CLAIM_APPROVED)
                .title("t")
                .message("m")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        service.markAsRead(1L, 10L);

        assertEquals(true, n.getIsRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllAsRead_shouldOnlyToggleUnreadOnes() {
        InsuranceNotification unread = InsuranceNotification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.CLAIM_APPROVED)
                .title("t1")
                .message("m1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        InsuranceNotification alreadyRead = InsuranceNotification.builder()
                .id(2L)
                .userId(10L)
                .type(NotificationType.CLAIM_APPROVED)
                .title("t2")
                .message("m2")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(unread, alreadyRead));

        service.markAllAsRead(10L);

        assertEquals(true, unread.getIsRead());
        assertEquals(true, alreadyRead.getIsRead());
        verify(notificationRepository).saveAll(List.of(unread, alreadyRead));
    }
}

