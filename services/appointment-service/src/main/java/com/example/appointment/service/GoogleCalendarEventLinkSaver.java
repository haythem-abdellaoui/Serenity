package com.example.appointment.service;

import com.example.appointment.entity.GoogleCalendarEventLink;
import com.example.appointment.repository.GoogleCalendarEventLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists user↔Google event id mappings in a short transaction so delete+insert cannot
 * violate the unique (user_id, appointment_id) constraint mid-flight.
 */
@Service
@RequiredArgsConstructor
public class GoogleCalendarEventLinkSaver {

    private final GoogleCalendarEventLinkRepository eventLinkRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceMapping(Long userId, Long appointmentId, String googleEventId) {
        eventLinkRepository.deleteByUserIdAndAppointmentId(userId, appointmentId);
        eventLinkRepository.flush();
        eventLinkRepository.save(GoogleCalendarEventLink.builder()
                .userId(userId)
                .appointmentId(appointmentId)
                .googleEventId(googleEventId)
                .build());
    }
}
