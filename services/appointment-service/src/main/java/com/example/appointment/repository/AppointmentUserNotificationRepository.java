package com.example.appointment.repository;

import com.example.appointment.entity.AppointmentUserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentUserNotificationRepository extends JpaRepository<AppointmentUserNotification, Long> {

    List<AppointmentUserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
