package com.example.appointment.service;

import com.example.appointment.config.GoogleCalendarProperties;
import com.example.appointment.dto.GoogleCalendarOAuthCompleteRequest;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.GoogleCalendarCredentialRepository;
import com.example.appointment.repository.GoogleCalendarEventLinkRepository;
import com.example.appointment.integration.UserDirectoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleCalendarIntegrationServiceTest {

    @Mock
    private GoogleCalendarProperties properties;
    @Mock
    private GoogleCalendarCredentialRepository credentialRepository;
    @Mock
    private GoogleCalendarEventLinkRepository eventLinkRepository;
    @Mock
    private GoogleCalendarEventLinkSaver eventLinkSaver;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserDirectoryClient userDirectoryClient;

    @InjectMocks
    private GoogleCalendarIntegrationService service;

    @BeforeEach
    void defaults() {
        // Keep setup empty to avoid unnecessary Mockito stubs in tests that do not need properties.
    }

    @Test
    void isConfigured_returnsFalseWhenClientSecretMissing() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getClientId()).thenReturn("cid");
        when(properties.getClientSecret()).thenReturn("");
        when(properties.getRedirectUri()).thenReturn("http://localhost:4200/appointments/oauth/calendar");
        assertFalse(service.isConfigured());
    }

    @Test
    void status_reportsConnectedWhenCredentialExists() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getClientId()).thenReturn("cid");
        when(properties.getClientSecret()).thenReturn("secret");
        when(properties.getRedirectUri()).thenReturn("http://localhost:4200/appointments/oauth/calendar");
        when(credentialRepository.findById(11L)).thenReturn(Optional.of(new com.example.appointment.entity.GoogleCalendarCredential()));
        var status = service.status(11L);
        assertTrue(status.isConfigured());
        assertTrue(status.isConnected());
    }

    @Test
    void buildAuthorizeUrl_includesGoogleOAuthBase() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getClientId()).thenReturn("cid");
        when(properties.getClientSecret()).thenReturn("secret");
        when(properties.getRedirectUri()).thenReturn("http://localhost:4200/appointments/oauth/calendar");
        var response = service.buildAuthorizeUrl("/appointments/list");
        assertTrue(response.getAuthorizeUrl().startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
        assertEquals("/appointments/list", response.getReturnTo());
    }

    @Test
    void decodeReturnTo_fallsBackForInvalidState() {
        assertEquals("/appointments/list", GoogleCalendarIntegrationService.decodeReturnTo("###"));
    }

    @Test
    void completeOAuth_rejectsMissingCode() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getClientId()).thenReturn("cid");
        when(properties.getClientSecret()).thenReturn("secret");
        when(properties.getRedirectUri()).thenReturn("http://localhost:4200/appointments/oauth/calendar");
        GoogleCalendarOAuthCompleteRequest req = new GoogleCalendarOAuthCompleteRequest();
        req.setCode(" ");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.completeOAuth(1L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
