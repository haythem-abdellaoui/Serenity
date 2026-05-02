package com.example.appointment.repository;

import com.example.appointment.entity.GoogleCalendarEventLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarEventLinkRepository extends JpaRepository<GoogleCalendarEventLink, Long> {

    Optional<GoogleCalendarEventLink> findByUserIdAndAppointmentId(Long userId, Long appointmentId);

    void deleteByUserIdAndAppointmentId(Long userId, Long appointmentId);

    void deleteByAppointmentId(Long appointmentId);
}
