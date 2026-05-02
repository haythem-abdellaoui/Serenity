package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.MedicalRecordMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.service.IMedicalRecordService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordServiceImpl implements IMedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalRecordResponseDTO> getAllRecords(Pageable pageable, Long doctorId, boolean isAdmin) {
        return (isAdmin
                ? medicalRecordRepository.findAll(pageable)
                : medicalRecordRepository.findAllByDoctorId(doctorId, pageable))
                .map(medicalRecordMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponseDTO getRecordById(Long id, Long doctorId, boolean isAdmin) {
        MedicalRecord record = (isAdmin
                ? medicalRecordRepository.findById(id)
                : medicalRecordRepository.findByIdAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", id));
        return medicalRecordMapper.toResponseDTO(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDTO> getRecordsByPatientId(Long patientId, Long doctorId, boolean isAdmin) {
        return (isAdmin
                ? medicalRecordRepository.findByPatientId(patientId)
                : medicalRecordRepository.findByPatientIdAndDoctorId(patientId, doctorId))
                .stream()
                .map(medicalRecordMapper::toResponseDTO)
                .toList();
    }

    @Override
    public MedicalRecordResponseDTO createRecord(MedicalRecordRequestDTO requestDTO, Long doctorId, boolean isAdmin) {
        MedicalRecord record = medicalRecordMapper.toEntity(requestDTO);
        record.setDoctorId(resolveDoctorId(requestDTO.getDoctorId(), doctorId, isAdmin));
        MedicalRecord saved = medicalRecordRepository.save(record);
        return medicalRecordMapper.toResponseDTO(saved);
    }

    @Override
    public MedicalRecordResponseDTO updateRecord(Long id, MedicalRecordRequestDTO requestDTO, Long doctorId, boolean isAdmin) {
        MedicalRecord record = (isAdmin
                ? medicalRecordRepository.findById(id)
                : medicalRecordRepository.findByIdAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", id));
        
        medicalRecordMapper.updateEntityFromDTO(requestDTO, record);
        if (requestDTO.getPatientId() != null) {
            record.setPatientId(requestDTO.getPatientId());
        }
        record.setDoctorId(resolveDoctorId(requestDTO.getDoctorId(), doctorId, isAdmin));
        MedicalRecord updated = medicalRecordRepository.save(record);
        return medicalRecordMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteRecord(Long id, Long doctorId, boolean isAdmin) {
        MedicalRecord record = (isAdmin
                ? medicalRecordRepository.findById(id)
                : medicalRecordRepository.findByIdAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", id));
        medicalRecordRepository.delete(record);
    }

    private Long resolveDoctorId(Long doctorIdFromRequest, Long authenticatedUserId, boolean isAdmin) {
        if (isAdmin && doctorIdFromRequest != null) {
            return doctorIdFromRequest;
        }
        return authenticatedUserId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDTO> searchRecords(String diagnosis, String status, String severity, Long doctorId, boolean isAdmin) {
        tn.esprit.arctic.derbelmicroservice.entity.enums.Severity sev = null;
        if (severity != null && !severity.isBlank()) {
            try {
                sev = tn.esprit.arctic.derbelmicroservice.entity.enums.Severity.valueOf(severity.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        String diagParam = (diagnosis != null && !diagnosis.isBlank()) ? diagnosis : null;
        String statusParam = (status != null && !status.isBlank()) ? status : null;

        List<MedicalRecord> results = isAdmin
                ? medicalRecordRepository.search(diagParam, statusParam, sev)
                : medicalRecordRepository.searchByDoctor(doctorId, diagParam, statusParam, sev);
        return results.stream().map(medicalRecordMapper::toResponseDTO).toList();
    }

    // ── JPQL Complexe: Dossiers avec traitement actif ──
    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDTO> getRecordsWithActiveTreatment(Long patientId, Long doctorId, boolean isAdmin) {
        List<MedicalRecord> records = isAdmin
                ? medicalRecordRepository.findRecordsWithActiveTreatment(patientId)
                : medicalRecordRepository.findRecordsWithActiveTreatmentByDoctor(patientId, doctorId);
        return records.stream().map(medicalRecordMapper::toResponseDTO).toList();
    }
}

