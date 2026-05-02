package com.example.appointment.service;

import com.example.appointment.entity.GoogleCalendarEventLink;
import com.example.appointment.repository.GoogleCalendarEventLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarEventLinkSaverTest {

    @Mock
    private GoogleCalendarEventLinkRepository eventLinkRepository;

    @InjectMocks
    private GoogleCalendarEventLinkSaver saver;

    @Test
    void replaceMapping_deletesFlushesAndSavesNewLink() {
        saver.replaceMapping(9L, 15L, "google-event-1");

        verify(eventLinkRepository).deleteByUserIdAndAppointmentId(9L, 15L);
        verify(eventLinkRepository).flush();
        ArgumentCaptor<GoogleCalendarEventLink> captor = ArgumentCaptor.forClass(GoogleCalendarEventLink.class);
        verify(eventLinkRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals(15L, captor.getValue().getAppointmentId());
        assertEquals("google-event-1", captor.getValue().getGoogleEventId());
    }
}
