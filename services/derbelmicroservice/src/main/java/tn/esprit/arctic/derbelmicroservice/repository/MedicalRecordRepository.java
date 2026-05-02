package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Page<MedicalRecord> findAll(Pageable pageable);

    Page<MedicalRecord> findAllByDoctorId(Long doctorId, Pageable pageable);

    Optional<MedicalRecord> findByIdAndDoctorId(Long id, Long doctorId);

    List<MedicalRecord> findByPatientId(Long patientId);

    List<MedicalRecord> findByPatientIdAndDoctorId(Long patientId, Long doctorId);

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE (:diagnosis IS NULL OR LOWER(m.diagnosis) LIKE LOWER(CONCAT('%',:diagnosis,'%'))) " +
           "AND (:status IS NULL OR LOWER(m.status) = LOWER(:status)) " +
           "AND (:severity IS NULL OR m.severity = :severity)")
    List<MedicalRecord> search(@Param("diagnosis") String diagnosis,
                               @Param("status") String status,
                               @Param("severity") tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity);

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.doctorId = :doctorId " +
           "AND (:diagnosis IS NULL OR LOWER(m.diagnosis) LIKE LOWER(CONCAT('%',:diagnosis,'%'))) " +
           "AND (:status IS NULL OR LOWER(m.status) = LOWER(:status)) " +
           "AND (:severity IS NULL OR m.severity = :severity)")
    List<MedicalRecord> searchByDoctor(@Param("doctorId") Long doctorId,
                                       @Param("diagnosis") String diagnosis,
                                       @Param("status") String status,
                                       @Param("severity") tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity);

    long countByStatusIgnoreCase(String status);

    long countByDoctorIdAndStatusIgnoreCase(Long doctorId, String status);

    long countBySeverity(tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity);

    long countByDoctorIdAndSeverity(Long doctorId, tn.esprit.arctic.derbelmicroservice.entity.enums.Severity severity);

    // ── JPQL Complexe (JOIN entre MedicalRecord et Prescription) ──
    @Query("SELECT DISTINCT m FROM MedicalRecord m " +
           "JOIN m.prescriptions p " +
           "WHERE m.patientId = :patientId AND p.status = 'ACTIVE'")
    List<MedicalRecord> findRecordsWithActiveTreatment(@Param("patientId") Long patientId);

    @Query("SELECT DISTINCT m FROM MedicalRecord m " +
           "JOIN m.prescriptions p " +
           "WHERE m.patientId = :patientId AND m.doctorId = :doctorId AND p.status = 'ACTIVE'")
    List<MedicalRecord> findRecordsWithActiveTreatmentByDoctor(@Param("patientId") Long patientId,
                                                                @Param("doctorId") Long doctorId);
}

