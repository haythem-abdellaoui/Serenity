package tn.esprit.arctic.derbelmicroservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;


@Component
public class MedicalRecordMapper {

    public MedicalRecordResponseDTO toResponseDTO(MedicalRecord record) {
        return MedicalRecordResponseDTO.builder()
                .id(record.getId())
                .diagnosis(record.getDiagnosis())
                .notes(record.getNotes())
                .date(record.getDate())
                .severity(record.getSeverity())
                .status(record.getStatus())
                .patientId(record.getPatientId())
                .patientFirstName(null)
                .patientLastName(null)
                .doctorId(record.getDoctorId())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    public MedicalRecord toEntity(MedicalRecordRequestDTO dto) {
        return MedicalRecord.builder()
                .diagnosis(dto.getDiagnosis())
                .notes(dto.getNotes())
                .date(dto.getDate())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .patientId(dto.getPatientId())
                .build();
    }

    public void updateEntityFromDTO(MedicalRecordRequestDTO dto, MedicalRecord record) {
        record.setDiagnosis(dto.getDiagnosis());
        record.setNotes(dto.getNotes());
        record.setDate(dto.getDate());
        record.setSeverity(dto.getSeverity());
        record.setStatus(dto.getStatus());
    }
}
