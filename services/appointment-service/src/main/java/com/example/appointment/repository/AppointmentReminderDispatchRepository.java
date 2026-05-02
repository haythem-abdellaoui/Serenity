package com.example.appointment.repository;

import com.example.appointment.entity.AppointmentReminderKind;
import com.example.appointment.entity.AppointmentReminderDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentReminderDispatchRepository extends JpaRepository<AppointmentReminderDispatch, Long> {

    boolean existsByAppointmentIdAndReminderKind(Long appointmentId, AppointmentReminderKind kind);

    void deleteByAppointmentId(Long appointmentId);
}
