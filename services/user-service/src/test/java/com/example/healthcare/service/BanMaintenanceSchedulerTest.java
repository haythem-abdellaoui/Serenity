package com.example.healthcare.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BanMaintenanceSchedulerTest {

    @Mock private BanMaintenanceService banMaintenanceService;
    @InjectMocks private BanMaintenanceScheduler scheduler;

    @Test
    void cleanupExpiredBans_callsService() {
        when(banMaintenanceService.unbanExpiredUsers()).thenReturn(0);
        scheduler.cleanupExpiredBans();
        verify(banMaintenanceService).unbanExpiredUsers();
    }
}

