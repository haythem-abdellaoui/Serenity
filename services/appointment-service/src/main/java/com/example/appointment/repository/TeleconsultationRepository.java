package com.example.appointment.repository;

import com.example.appointment.entity.Teleconsultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeleconsultationRepository extends JpaRepository<Teleconsultation, Long> {

    Optional<Teleconsultation> findByAppointment_Id(Long appointmentId);
}
