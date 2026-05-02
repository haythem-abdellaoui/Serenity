package com.example.healthcare.service;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BanMaintenanceServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private BanMaintenanceService service;

    @Test
    void unbanExpiredUsers_whenNone_returns0() {
        when(userRepository.findByIsPermanentlyBannedFalseAndBannedUntilLessThanEqual(any(Date.class)))
                .thenReturn(List.of());

        int cleaned = service.unbanExpiredUsers();

        assertThat(cleaned).isZero();
        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void unbanExpiredUsers_whenEligible_unbansAndSaves() {
        User u1 = new User();
        u1.setIsPermanentlyBanned(false);
        u1.setBannedUntil(new Date());

        User u2 = new User();
        u2.setIsPermanentlyBanned(false);
        u2.setBannedUntil(new Date());

        when(userRepository.findByIsPermanentlyBannedFalseAndBannedUntilLessThanEqual(any(Date.class)))
                .thenReturn(List.of(u1, u2));

        int cleaned = service.unbanExpiredUsers();

        assertThat(cleaned).isEqualTo(2);
        assertThat(u1.getBannedUntil()).isNull();
        assertThat(u2.getBannedUntil()).isNull();
        verify(userRepository).saveAll(List.of(u1, u2));
    }
}

