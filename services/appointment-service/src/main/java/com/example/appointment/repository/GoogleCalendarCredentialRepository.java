package com.example.appointment.repository;

import com.example.appointment.entity.GoogleCalendarCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarCredentialRepository extends JpaRepository<GoogleCalendarCredential, Long> {
}
