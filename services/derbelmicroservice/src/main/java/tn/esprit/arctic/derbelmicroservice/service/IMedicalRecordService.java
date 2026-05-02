package tn.esprit.arctic.derbelmicroservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;

import java.util.List;

public interface IMedicalRecordService {

    Page<MedicalRecordResponseDTO> getAllRecords(Pageable pageable, Long doctorId, boolean isAdmin);

    MedicalRecordResponseDTO getRecordById(Long id, Long doctorId, boolean isAdmin);

    List<MedicalRecordResponseDTO> getRecordsByPatientId(Long patientId, Long doctorId, boolean isAdmin);

    MedicalRecordResponseDTO createRecord(MedicalRecordRequestDTO requestDTO, Long doctorId, boolean isAdmin);

    MedicalRecordResponseDTO updateRecord(Long id, MedicalRecordRequestDTO requestDTO, Long doctorId, boolean isAdmin);

    void deleteRecord(Long id, Long doctorId, boolean isAdmin);

    List<MedicalRecordResponseDTO> searchRecords(String diagnosis, String status, String severity, Long doctorId, boolean isAdmin);

    // ── JPQL Complexe: Dossiers avec traitement actif ──
    List<MedicalRecordResponseDTO> getRecordsWithActiveTreatment(Long patientId, Long doctorId, boolean isAdmin);
}
