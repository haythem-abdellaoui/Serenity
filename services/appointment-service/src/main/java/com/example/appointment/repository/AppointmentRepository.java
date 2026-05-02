package com.example.appointment.repository;

import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a from Appointment a
            where a.patientUserId = :userId or a.doctorUserId = :userId
            order by a.appointmentDate desc, a.timeSlot desc
            """)
    List<Appointment> findMineSorted(@Param("userId") Long userId);

    @Query("""
            select a from Appointment a
            order by a.appointmentDate desc, a.timeSlot desc
            """)
    List<Appointment> findAllSorted();

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.doctorUserId = :doctorUserId
              and a.appointmentDate = :appointmentDate
              and a.timeSlot = :timeSlot
              and a.status in :statuses
            """)
    boolean existsAtDoctorSlotWithStatuses(
            @Param("doctorUserId") Long doctorUserId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("timeSlot") String timeSlot,
            @Param("statuses") Collection<AppointmentStatus> statuses);

    @Query("""
            select a from Appointment a
            where a.id = :id and a.patientUserId = :patientUserId
            """)
    Optional<Appointment> findAccessibleByPatient(@Param("id") Long id, @Param("patientUserId") Long patientUserId);

    @Query("""
            select a from Appointment a
            where a.id = :id and a.doctorUserId = :doctorUserId
            """)
    Optional<Appointment> findAccessibleByDoctor(@Param("id") Long id, @Param("doctorUserId") Long doctorUserId);

    @Query("""
            select a from Appointment a
            where a.status = :status and a.appointmentDate >= :minDate
            order by a.appointmentDate asc, a.timeSlot asc
            """)
    List<Appointment> findByStatusFromDate(
            @Param("status") AppointmentStatus status,
            @Param("minDate") LocalDate minDate);

    @Query("""
            select a from Appointment a
            where a.doctorUserId = :doctorUserId
              and a.appointmentDate between :fromInclusive and :toInclusive
              and a.status in :statuses
            order by a.appointmentDate asc, a.timeSlot asc
            """)
    List<Appointment> findDoctorCalendarRange(
            @Param("doctorUserId") Long doctorUserId,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive,
            @Param("statuses") Collection<AppointmentStatus> statuses);

    @Query("""
            select a from Appointment a
            where a.patientUserId = :patientUserId
              and a.appointmentDate between :fromInclusive and :toInclusive
              and a.status in :statuses
            order by a.appointmentDate asc, a.timeSlot asc
            """)
    List<Appointment> findPatientCalendarRange(
            @Param("patientUserId") Long patientUserId,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive,
            @Param("statuses") Collection<AppointmentStatus> statuses);
}
