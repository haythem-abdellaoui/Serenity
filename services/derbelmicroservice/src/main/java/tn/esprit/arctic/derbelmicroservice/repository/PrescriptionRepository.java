package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @EntityGraph(attributePaths = {"medicalRecord", "items", "items.medicine"})
    @Query("SELECT p FROM Prescription p")
    Page<Prescription> findAllWithItems(Pageable pageable);

    @EntityGraph(attributePaths = {"medicalRecord", "items", "items.medicine"})
    @Query("SELECT p FROM Prescription p WHERE p.doctorId = :doctorId")
    Page<Prescription> findAllByDoctorIdWithItems(@Param("doctorId") Long doctorId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.medicine " +
           "JOIN FETCH p.medicalRecord WHERE p.id = :id")
    Optional<Prescription> findByIdFull(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.medicine " +
           "JOIN FETCH p.medicalRecord WHERE p.id = :id AND p.doctorId = :doctorId")
    Optional<Prescription> findByIdFullAndDoctorId(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.medicine " +
           "JOIN FETCH p.medicalRecord mr WHERE mr.id = :recordId")
    List<Prescription> findByMedicalRecordId(@Param("recordId") Long medicalRecordId);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.medicine " +
           "JOIN FETCH p.medicalRecord mr WHERE mr.id = :recordId AND p.doctorId = :doctorId")
    List<Prescription> findByMedicalRecordIdAndDoctorId(@Param("recordId") Long recordId,
                                                        @Param("doctorId") Long doctorId);

    long countByStatusIgnoreCase(String status);

    long countByDoctorIdAndStatusIgnoreCase(Long doctorId, String status);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.medicine " +
           "JOIN FETCH p.medicalRecord " +
           "WHERE (:medicationName IS NULL OR LOWER(i.medicine.name) LIKE LOWER(CONCAT('%',:medicationName,'%'))) " +
           "AND (:status IS NULL OR LOWER(p.status) = LOWER(:status))")
    List<Prescription> search(@Param("medicationName") String medicationName,
                              @Param("status") String status);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.medicine " +
           "JOIN FETCH p.medicalRecord " +
           "WHERE p.doctorId = :doctorId " +
           "AND (:medicationName IS NULL OR LOWER(i.medicine.name) LIKE LOWER(CONCAT('%',:medicationName,'%'))) " +
           "AND (:status IS NULL OR LOWER(p.status) = LOWER(:status))")
    List<Prescription> searchByDoctor(@Param("doctorId") Long doctorId,
                                      @Param("medicationName") String medicationName,
                                      @Param("status") String status);

    // ── Scheduler: find all active prescriptions with items loaded ──
    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.items i " +
           "WHERE p.status = :status")
    List<Prescription> findByStatusWithItems(@Param("status") String status);

    // ── Keywords Complexes (traverse Prescription → MedicalRecord, 2 tables) ──
    List<Prescription> findByPatientIdAndMedicalRecord_Severity(Long patientId, tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity);

    List<Prescription> findByPatientIdAndMedicalRecord_SeverityAndDoctorId(Long patientId, tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity, Long doctorId);
}
