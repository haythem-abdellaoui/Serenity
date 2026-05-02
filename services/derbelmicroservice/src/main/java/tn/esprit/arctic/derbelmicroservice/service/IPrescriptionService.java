package tn.esprit.arctic.derbelmicroservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;

import java.util.List;

public interface IPrescriptionService {

    Page<PrescriptionResponseDTO> getAllPrescriptions(Pageable pageable, Long doctorId, boolean isAdmin);

    PrescriptionResponseDTO getPrescriptionById(Long id, Long doctorId, boolean isAdmin);

    List<PrescriptionResponseDTO> getPrescriptionsByRecordId(Long recordId, Long doctorId, boolean isAdmin);

    PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO requestDTO, Long doctorId, boolean isAdmin);

    PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO requestDTO, Long doctorId, boolean isAdmin);

    void deletePrescription(Long id, Long doctorId, boolean isAdmin);

    List<PrescriptionResponseDTO> searchPrescriptions(String medicationName, String status, Long doctorId, boolean isAdmin);

    // ── Keywords Complexes: Prescriptions critiques ──
    List<PrescriptionResponseDTO> getCriticalPrescriptions(Long patientId, Long doctorId, boolean isAdmin);
}
