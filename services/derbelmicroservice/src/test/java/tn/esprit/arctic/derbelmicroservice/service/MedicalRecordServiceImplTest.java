package tn.esprit.arctic.derbelmicroservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.enums.Severity;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.MedicalRecordMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.service.impl.MedicalRecordServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour MedicalRecordServiceImpl.
 * Utilise Mockito pour simuler le repository et le mapper.
 */
@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private MedicalRecordMapper medicalRecordMapper;

    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    // ── Données de test réutilisables ──
    private MedicalRecord sampleRecord;
    private MedicalRecordResponseDTO sampleResponseDTO;
    private MedicalRecordRequestDTO sampleRequestDTO;

    private static final Long DOCTOR_ID = 1L;
    private static final Long PATIENT_ID = 10L;
    private static final Long RECORD_ID = 100L;

    @BeforeEach
    void setUp() {
        sampleRecord = MedicalRecord.builder()
                .id(RECORD_ID)
                .diagnosis("G43.9 - Migraine, unspecified")
                .notes("Patient stable")
                .date(LocalDate.of(2026, 4, 25))
                .severity(Severity.MEDIUM)
                .status("ACTIVE")
                .patientId(PATIENT_ID)
                .doctorId(DOCTOR_ID)
                .build();

        sampleResponseDTO = MedicalRecordResponseDTO.builder()
                .id(RECORD_ID)
                .diagnosis("G43.9 - Migraine, unspecified")
                .notes("Patient stable")
                .date(LocalDate.of(2026, 4, 25))
                .severity(Severity.MEDIUM)
                .status("ACTIVE")
                .patientId(PATIENT_ID)
                .doctorId(DOCTOR_ID)
                .build();

        sampleRequestDTO = MedicalRecordRequestDTO.builder()
                .diagnosis("G43.9 - Migraine, unspecified")
                .notes("Patient stable")
                .date(LocalDate.of(2026, 4, 25))
                .severity(Severity.MEDIUM)
                .status("ACTIVE")
                .patientId(PATIENT_ID)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  GET ALL RECORDS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllRecords()")
    class GetAllRecords {

        @Test
        @DisplayName("Admin → doit retourner TOUS les dossiers (sans filtre docteur)")
        void getAllRecords_asAdmin_returnsAll() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalRecord> page = new PageImpl<>(List.of(sampleRecord));

            when(medicalRecordRepository.findAll(pageable)).thenReturn(page);
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            Page<MedicalRecordResponseDTO> result = medicalRecordService.getAllRecords(pageable, DOCTOR_ID, true);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDiagnosis()).isEqualTo("G43.9 - Migraine, unspecified");
            verify(medicalRecordRepository).findAll(pageable);
            verify(medicalRecordRepository, never()).findAllByDoctorId(anyLong(), any());
        }

        @Test
        @DisplayName("Docteur → doit retourner uniquement ses dossiers")
        void getAllRecords_asDoctor_returnsOnlyOwn() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalRecord> page = new PageImpl<>(List.of(sampleRecord));

            when(medicalRecordRepository.findAllByDoctorId(DOCTOR_ID, pageable)).thenReturn(page);
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            Page<MedicalRecordResponseDTO> result = medicalRecordService.getAllRecords(pageable, DOCTOR_ID, false);

            assertThat(result.getContent()).hasSize(1);
            verify(medicalRecordRepository).findAllByDoctorId(DOCTOR_ID, pageable);
            verify(medicalRecordRepository, never()).findAll(any(Pageable.class));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET RECORD BY ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRecordById()")
    class GetRecordById {

        @Test
        @DisplayName("Dossier existant → doit retourner le dossier")
        void getRecordById_existingId_returnsRecord() {
            when(medicalRecordRepository.findByIdAndDoctorId(RECORD_ID, DOCTOR_ID))
                    .thenReturn(Optional.of(sampleRecord));
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            MedicalRecordResponseDTO result = medicalRecordService.getRecordById(RECORD_ID, DOCTOR_ID, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RECORD_ID);
            assertThat(result.getSeverity()).isEqualTo(Severity.MEDIUM);
        }

        @Test
        @DisplayName("Dossier inexistant → doit lever ResourceNotFoundException")
        void getRecordById_notFound_throwsException() {
            when(medicalRecordRepository.findByIdAndDoctorId(999L, DOCTOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> medicalRecordService.getRecordById(999L, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MedicalRecord");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET RECORDS BY PATIENT ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRecordsByPatientId()")
    class GetRecordsByPatientId {

        @Test
        @DisplayName("Doit retourner les dossiers du patient filtré par docteur")
        void getRecordsByPatientId_asDoctor_returnsFiltered() {
            when(medicalRecordRepository.findByPatientIdAndDoctorId(PATIENT_ID, DOCTOR_ID))
                    .thenReturn(List.of(sampleRecord));
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.getRecordsByPatientId(PATIENT_ID, DOCTOR_ID, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientId()).isEqualTo(PATIENT_ID);
        }

        @Test
        @DisplayName("Admin → doit retourner TOUS les dossiers du patient")
        void getRecordsByPatientId_asAdmin_returnsAll() {
            when(medicalRecordRepository.findByPatientId(PATIENT_ID))
                    .thenReturn(List.of(sampleRecord));
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.getRecordsByPatientId(PATIENT_ID, DOCTOR_ID, true);

            assertThat(result).hasSize(1);
            verify(medicalRecordRepository).findByPatientId(PATIENT_ID);
            verify(medicalRecordRepository, never()).findByPatientIdAndDoctorId(anyLong(), anyLong());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CREATE RECORD
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createRecord()")
    class CreateRecord {

        @Test
        @DisplayName("Doit créer un dossier et assigner le doctorId automatiquement")
        void createRecord_asDoctor_setsDoctorId() {
            when(medicalRecordMapper.toEntity(sampleRequestDTO)).thenReturn(sampleRecord);
            when(medicalRecordRepository.save(sampleRecord)).thenReturn(sampleRecord);
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            MedicalRecordResponseDTO result =
                    medicalRecordService.createRecord(sampleRequestDTO, DOCTOR_ID, false);

            assertThat(result).isNotNull();
            assertThat(result.getDiagnosis()).isEqualTo("G43.9 - Migraine, unspecified");
            verify(medicalRecordRepository).save(sampleRecord);
            // Vérifie que le doctorId est bien défini par le service (pas par le DTO)
            assertThat(sampleRecord.getDoctorId()).isEqualTo(DOCTOR_ID);
        }

        @Test
        @DisplayName("Admin avec doctorId dans le DTO → doit utiliser le doctorId du DTO")
        void createRecord_asAdmin_usesDtoDoctor() {
            Long otherDoctorId = 42L;
            sampleRequestDTO.setDoctorId(otherDoctorId);

            when(medicalRecordMapper.toEntity(sampleRequestDTO)).thenReturn(sampleRecord);
            when(medicalRecordRepository.save(sampleRecord)).thenReturn(sampleRecord);
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            medicalRecordService.createRecord(sampleRequestDTO, DOCTOR_ID, true);

            assertThat(sampleRecord.getDoctorId()).isEqualTo(otherDoctorId);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UPDATE RECORD
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateRecord()")
    class UpdateRecord {

        @Test
        @DisplayName("Dossier existant → doit mettre à jour et sauvegarder")
        void updateRecord_existingId_updatesAndSaves() {
            when(medicalRecordRepository.findByIdAndDoctorId(RECORD_ID, DOCTOR_ID))
                    .thenReturn(Optional.of(sampleRecord));
            when(medicalRecordRepository.save(sampleRecord)).thenReturn(sampleRecord);
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            MedicalRecordResponseDTO result =
                    medicalRecordService.updateRecord(RECORD_ID, sampleRequestDTO, DOCTOR_ID, false);

            assertThat(result).isNotNull();
            verify(medicalRecordMapper).updateEntityFromDTO(sampleRequestDTO, sampleRecord);
            verify(medicalRecordRepository).save(sampleRecord);
        }

        @Test
        @DisplayName("Dossier inexistant → doit lever ResourceNotFoundException")
        void updateRecord_notFound_throwsException() {
            when(medicalRecordRepository.findByIdAndDoctorId(999L, DOCTOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    medicalRecordService.updateRecord(999L, sampleRequestDTO, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DELETE RECORD
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteRecord()")
    class DeleteRecord {

        @Test
        @DisplayName("Dossier existant → doit supprimer avec succès")
        void deleteRecord_existingId_deletesSuccessfully() {
            when(medicalRecordRepository.findByIdAndDoctorId(RECORD_ID, DOCTOR_ID))
                    .thenReturn(Optional.of(sampleRecord));

            medicalRecordService.deleteRecord(RECORD_ID, DOCTOR_ID, false);

            verify(medicalRecordRepository).delete(sampleRecord);
        }

        @Test
        @DisplayName("Dossier inexistant → doit lever ResourceNotFoundException")
        void deleteRecord_notFound_throwsException() {
            when(medicalRecordRepository.findByIdAndDoctorId(999L, DOCTOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    medicalRecordService.deleteRecord(999L, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SEARCH RECORDS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchRecords()")
    class SearchRecords {

        @Test
        @DisplayName("Recherche par diagnostic → doit retourner les résultats filtrés")
        void searchRecords_byDiagnosis_returnsFiltered() {
            when(medicalRecordRepository.searchByDoctor(eq(DOCTOR_ID), eq("migraine"), isNull(), isNull()))
                    .thenReturn(List.of(sampleRecord));
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.searchRecords("migraine", null, null, DOCTOR_ID, false);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Recherche par sévérité HIGH → doit convertir le String en Enum")
        void searchRecords_bySeverity_convertsEnum() {
            when(medicalRecordRepository.searchByDoctor(eq(DOCTOR_ID), isNull(), isNull(), eq(Severity.HIGH)))
                    .thenReturn(List.of());
            
            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.searchRecords(null, null, "HIGH", DOCTOR_ID, false);

            assertThat(result).isEmpty();
            verify(medicalRecordRepository).searchByDoctor(DOCTOR_ID, null, null, Severity.HIGH);
        }

        @Test
        @DisplayName("Sévérité invalide → doit être ignorée (pas d'exception)")
        void searchRecords_invalidSeverity_ignoresGracefully() {
            when(medicalRecordRepository.searchByDoctor(eq(DOCTOR_ID), isNull(), isNull(), isNull()))
                    .thenReturn(List.of());

            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.searchRecords(null, null, "INVALID", DOCTOR_ID, false);

            assertThat(result).isEmpty();
            // Vérifie que le severity null est passé (car "INVALID" n'est pas un Enum valide)
            verify(medicalRecordRepository).searchByDoctor(DOCTOR_ID, null, null, null);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  JPQL COMPLEXE: ACTIVE TREATMENT
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRecordsWithActiveTreatment()")
    class GetRecordsWithActiveTreatment {

        @Test
        @DisplayName("Admin → appelle findRecordsWithActiveTreatment (sans filtre docteur)")
        void getRecordsWithActiveTreatment_asAdmin_noFilterDoctor() {
            when(medicalRecordRepository.findRecordsWithActiveTreatment(PATIENT_ID))
                    .thenReturn(List.of(sampleRecord));
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.getRecordsWithActiveTreatment(PATIENT_ID, DOCTOR_ID, true);

            assertThat(result).hasSize(1);
            verify(medicalRecordRepository).findRecordsWithActiveTreatment(PATIENT_ID);
        }

        @Test
        @DisplayName("Docteur → appelle findRecordsWithActiveTreatmentByDoctor")
        void getRecordsWithActiveTreatment_asDoctor_filtersDoctor() {
            when(medicalRecordRepository.findRecordsWithActiveTreatmentByDoctor(PATIENT_ID, DOCTOR_ID))
                    .thenReturn(List.of(sampleRecord));
            when(medicalRecordMapper.toResponseDTO(sampleRecord)).thenReturn(sampleResponseDTO);

            List<MedicalRecordResponseDTO> result =
                    medicalRecordService.getRecordsWithActiveTreatment(PATIENT_ID, DOCTOR_ID, false);

            assertThat(result).hasSize(1);
            verify(medicalRecordRepository).findRecordsWithActiveTreatmentByDoctor(PATIENT_ID, DOCTOR_ID);
        }
    }
}
