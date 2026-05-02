package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.PrescriptionMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.repository.PrescriptionRepository;
import tn.esprit.arctic.derbelmicroservice.service.IPrescriptionService;
import tn.esprit.arctic.derbelmicroservice.service.CloudinaryService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements IPrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionMapper prescriptionMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getAllPrescriptions(Pageable pageable, Long doctorId, boolean isAdmin) {
        return (isAdmin
                ? prescriptionRepository.findAllWithItems(pageable)
                : prescriptionRepository.findAllByDoctorIdWithItems(doctorId, pageable))
                .map(prescriptionMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionById(Long id, Long doctorId, boolean isAdmin) {
        Prescription prescription = (isAdmin
                ? prescriptionRepository.findByIdFull(id)
                : prescriptionRepository.findByIdFullAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        return prescriptionMapper.toResponseDTO(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPrescriptionsByRecordId(Long recordId, Long doctorId, boolean isAdmin) {
        if (!medicalRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("MedicalRecord", "id", recordId);
        }
        return (isAdmin
                ? prescriptionRepository.findByMedicalRecordId(recordId)
                : prescriptionRepository.findByMedicalRecordIdAndDoctorId(recordId, doctorId))
                .stream()
                .map(prescriptionMapper::toResponseDTO)
                .toList();
    }

    @Override
    public PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO requestDTO, Long doctorId, boolean isAdmin) {
        MedicalRecord record = medicalRecordRepository.findById(requestDTO.getMedicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", requestDTO.getMedicalRecordId()));

        Prescription prescription = prescriptionMapper.toEntity(requestDTO, record);
        prescription.setDoctorId(resolveDoctorId(requestDTO.getDoctorId(), doctorId, isAdmin));

        if (requestDTO.getImageBase64() != null && !requestDTO.getImageBase64().trim().isEmpty()) {
            try {
                String url = cloudinaryService.uploadBase64Image(requestDTO.getImageBase64());
                prescription.setImageUrl(url);
            } catch (Exception e) {
                // Log and continue, do not block prescription creation if image fails
                System.err.println("Warning: Cloudinary upload failed: " + e.getMessage());
            }
        }

        Prescription saved = prescriptionRepository.save(prescription);
        return prescriptionMapper.toResponseDTO(
                prescriptionRepository.findByIdFull(saved.getId()).orElse(saved));
    }

    @Override
    public PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO requestDTO, Long doctorId, boolean isAdmin) {
        Prescription prescription = (isAdmin
                ? prescriptionRepository.findByIdFull(id)
                : prescriptionRepository.findByIdFullAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        if (requestDTO.getMedicalRecordId() != null
                && !requestDTO.getMedicalRecordId().equals(prescription.getMedicalRecord().getId())) {
            MedicalRecord newRecord = medicalRecordRepository.findById(requestDTO.getMedicalRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", requestDTO.getMedicalRecordId()));
            prescription.setMedicalRecord(newRecord);
        }

        prescriptionMapper.updateEntity(requestDTO, prescription);
        prescription.setDoctorId(resolveDoctorId(requestDTO.getDoctorId(), doctorId, isAdmin));

        Prescription updated = prescriptionRepository.save(prescription);
        return prescriptionMapper.toResponseDTO(
                prescriptionRepository.findByIdFull(updated.getId()).orElse(updated));
    }

    @Override
    public void deletePrescription(Long id, Long doctorId, boolean isAdmin) {
        Prescription prescription = (isAdmin
                ? prescriptionRepository.findById(id)
                : prescriptionRepository.findByIdFullAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        prescriptionRepository.delete(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> searchPrescriptions(String medicationName, String status, Long doctorId, boolean isAdmin) {
        return (isAdmin
                ? prescriptionRepository.search(medicationName, status)
                : prescriptionRepository.searchByDoctor(doctorId, medicationName, status))
                .stream()
                .map(prescriptionMapper::toResponseDTO)
                .toList();
    }

    private Long resolveDoctorId(Long doctorIdFromRequest, Long authenticatedUserId, boolean isAdmin) {
        if (isAdmin && doctorIdFromRequest != null) {
            return doctorIdFromRequest;
        }
        return authenticatedUserId;
    }

    // ── Keywords Complexes: Prescriptions critiques (traverse 2 tables) ──
    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getCriticalPrescriptions(Long patientId, Long doctorId, boolean isAdmin) {
        List<Prescription> results = isAdmin
                ? prescriptionRepository.findByPatientIdAndMedicalRecord_Severity(patientId,
                    tn.esprit.arctic.derbelmicroservice.entity.enums.Severity.HIGH)
                : prescriptionRepository.findByPatientIdAndMedicalRecord_SeverityAndDoctorId(patientId,
                    tn.esprit.arctic.derbelmicroservice.entity.enums.Severity.HIGH, doctorId);
        return results.stream().map(prescriptionMapper::toResponseDTO).toList();
    }
}
