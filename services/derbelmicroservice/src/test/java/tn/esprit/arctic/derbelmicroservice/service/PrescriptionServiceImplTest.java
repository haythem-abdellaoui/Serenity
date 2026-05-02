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
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;
import tn.esprit.arctic.derbelmicroservice.entity.enums.Severity;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.PrescriptionMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.repository.PrescriptionRepository;
import tn.esprit.arctic.derbelmicroservice.service.CloudinaryService;
import tn.esprit.arctic.derbelmicroservice.service.impl.PrescriptionServiceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PrescriptionServiceImpl.
 * Couvre le CRUD, la recherche, l'upload Cloudinary,
 * et la requête keyword complexe (prescriptions critiques).
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PrescriptionMapper prescriptionMapper;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    // ── Données de test ──
    private MedicalRecord sampleRecord;
    private Prescription samplePrescription;
    private PrescriptionResponseDTO sampleResponseDTO;
    private PrescriptionRequestDTO sampleRequestDTO;

    private static final Long DOCTOR_ID = 1L;
    private static final Long PATIENT_ID = 10L;
    private static final Long RECORD_ID = 100L;
    private static final Long PRESCRIPTION_ID = 200L;

    @BeforeEach
    void setUp() {
        sampleRecord = MedicalRecord.builder()
                .id(RECORD_ID)
                .diagnosis("F32.1 - Major depressive disorder")
                .severity(Severity.MEDIUM)
                .status("ACTIVE")
                .patientId(PATIENT_ID)
                .doctorId(DOCTOR_ID)
                .date(LocalDate.now())
                .build();

        samplePrescription = Prescription.builder()
                .id(PRESCRIPTION_ID)
                .medicalRecord(sampleRecord)
                .patientId(PATIENT_ID)
                .doctorId(DOCTOR_ID)
                .status("ACTIVE")
                .items(new ArrayList<>())
                .build();

        sampleResponseDTO = PrescriptionResponseDTO.builder()
                .id(PRESCRIPTION_ID)
                .status("ACTIVE")
                .build();

        sampleRequestDTO = PrescriptionRequestDTO.builder()
                .medicalRecordId(RECORD_ID)
                .patientId(PATIENT_ID)
                .status("ACTIVE")
                .items(new ArrayList<>())
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  GET ALL PRESCRIPTIONS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllPrescriptions()")
    class GetAllPrescriptions {

        @Test
        @DisplayName("Admin → doit retourner TOUTES les prescriptions")
        void getAllPrescriptions_asAdmin_returnsAll() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Prescription> page = new PageImpl<>(List.of(samplePrescription));

            when(prescriptionRepository.findAllWithItems(pageable)).thenReturn(page);
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            Page<PrescriptionResponseDTO> result =
                    prescriptionService.getAllPrescriptions(pageable, DOCTOR_ID, true);

            assertThat(result.getContent()).hasSize(1);
            verify(prescriptionRepository).findAllWithItems(pageable);
            verify(prescriptionRepository, never()).findAllByDoctorIdWithItems(anyLong(), any());
        }

        @Test
        @DisplayName("Docteur → doit retourner uniquement ses prescriptions")
        void getAllPrescriptions_asDoctor_returnsOwn() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Prescription> page = new PageImpl<>(List.of(samplePrescription));

            when(prescriptionRepository.findAllByDoctorIdWithItems(DOCTOR_ID, pageable)).thenReturn(page);
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            Page<PrescriptionResponseDTO> result =
                    prescriptionService.getAllPrescriptions(pageable, DOCTOR_ID, false);

            assertThat(result.getContent()).hasSize(1);
            verify(prescriptionRepository).findAllByDoctorIdWithItems(DOCTOR_ID, pageable);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET PRESCRIPTION BY ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPrescriptionById()")
    class GetPrescriptionById {

        @Test
        @DisplayName("Prescription existante → doit retourner le DTO")
        void getPrescriptionById_existing_returnsPrescription() {
            when(prescriptionRepository.findByIdFullAndDoctorId(PRESCRIPTION_ID, DOCTOR_ID))
                    .thenReturn(Optional.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            PrescriptionResponseDTO result =
                    prescriptionService.getPrescriptionById(PRESCRIPTION_ID, DOCTOR_ID, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PRESCRIPTION_ID);
        }

        @Test
        @DisplayName("Prescription inexistante → doit lever ResourceNotFoundException")
        void getPrescriptionById_notFound_throwsException() {
            when(prescriptionRepository.findByIdFullAndDoctorId(999L, DOCTOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    prescriptionService.getPrescriptionById(999L, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Prescription");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET PRESCRIPTIONS BY RECORD ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPrescriptionsByRecordId()")
    class GetPrescriptionsByRecordId {

        @Test
        @DisplayName("Record existant → doit retourner les prescriptions liées")
        void getPrescriptionsByRecordId_existing_returnsList() {
            when(medicalRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(prescriptionRepository.findByMedicalRecordIdAndDoctorId(RECORD_ID, DOCTOR_ID))
                    .thenReturn(List.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            List<PrescriptionResponseDTO> result =
                    prescriptionService.getPrescriptionsByRecordId(RECORD_ID, DOCTOR_ID, false);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Record inexistant → doit lever ResourceNotFoundException")
        void getPrescriptionsByRecordId_recordNotFound_throwsException() {
            when(medicalRecordRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() ->
                    prescriptionService.getPrescriptionsByRecordId(999L, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MedicalRecord");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CREATE PRESCRIPTION
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPrescription()")
    class CreatePrescription {

        @Test
        @DisplayName("Création standard → doit sauvegarder et retourner la prescription")
        void createPrescription_validRequest_createsSuccessfully() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(sampleRecord));
            when(prescriptionMapper.toEntity(sampleRequestDTO, sampleRecord)).thenReturn(samplePrescription);
            when(prescriptionRepository.save(samplePrescription)).thenReturn(samplePrescription);
            when(prescriptionRepository.findByIdFull(PRESCRIPTION_ID))
                    .thenReturn(Optional.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            PrescriptionResponseDTO result =
                    prescriptionService.createPrescription(sampleRequestDTO, DOCTOR_ID, false);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(prescriptionRepository).save(samplePrescription);
        }

        @Test
        @DisplayName("MedicalRecord inexistant → doit lever ResourceNotFoundException")
        void createPrescription_recordNotFound_throwsException() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    prescriptionService.createPrescription(sampleRequestDTO, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MedicalRecord");

            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Avec image base64 → doit uploader vers Cloudinary")
        void createPrescription_withImage_uploadsToCloudinary() throws Exception {
            sampleRequestDTO.setImageBase64("data:image/png;base64,iVBOR...");

            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(sampleRecord));
            when(prescriptionMapper.toEntity(sampleRequestDTO, sampleRecord)).thenReturn(samplePrescription);
            when(cloudinaryService.uploadBase64Image(anyString())).thenReturn("https://cloudinary.com/image.png");
            when(prescriptionRepository.save(samplePrescription)).thenReturn(samplePrescription);
            when(prescriptionRepository.findByIdFull(PRESCRIPTION_ID))
                    .thenReturn(Optional.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            prescriptionService.createPrescription(sampleRequestDTO, DOCTOR_ID, false);

            verify(cloudinaryService).uploadBase64Image("data:image/png;base64,iVBOR...");
            assertThat(samplePrescription.getImageUrl()).isEqualTo("https://cloudinary.com/image.png");
        }

        @Test
        @DisplayName("Échec Cloudinary → ne doit PAS bloquer la création")
        void createPrescription_cloudinaryFails_continuesNormally() throws Exception {
            sampleRequestDTO.setImageBase64("data:image/png;base64,iVBOR...");

            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(sampleRecord));
            when(prescriptionMapper.toEntity(sampleRequestDTO, sampleRecord)).thenReturn(samplePrescription);
            when(cloudinaryService.uploadBase64Image(anyString())).thenThrow(new RuntimeException("Upload failed"));
            when(prescriptionRepository.save(samplePrescription)).thenReturn(samplePrescription);
            when(prescriptionRepository.findByIdFull(PRESCRIPTION_ID))
                    .thenReturn(Optional.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            // Ne doit PAS lever d'exception
            PrescriptionResponseDTO result =
                    prescriptionService.createPrescription(sampleRequestDTO, DOCTOR_ID, false);

            assertThat(result).isNotNull();
            verify(prescriptionRepository).save(samplePrescription);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DELETE PRESCRIPTION
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deletePrescription()")
    class DeletePrescription {

        @Test
        @DisplayName("Prescription existante → doit supprimer avec succès")
        void deletePrescription_existing_deletesSuccessfully() {
            when(prescriptionRepository.findByIdFullAndDoctorId(PRESCRIPTION_ID, DOCTOR_ID))
                    .thenReturn(Optional.of(samplePrescription));

            prescriptionService.deletePrescription(PRESCRIPTION_ID, DOCTOR_ID, false);

            verify(prescriptionRepository).delete(samplePrescription);
        }

        @Test
        @DisplayName("Prescription inexistante → doit lever ResourceNotFoundException")
        void deletePrescription_notFound_throwsException() {
            when(prescriptionRepository.findByIdFullAndDoctorId(999L, DOCTOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    prescriptionService.deletePrescription(999L, DOCTOR_ID, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SEARCH PRESCRIPTIONS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchPrescriptions()")
    class SearchPrescriptions {

        @Test
        @DisplayName("Recherche par nom de médicament → doit retourner les résultats")
        void searchPrescriptions_byMedName_returnsResults() {
            when(prescriptionRepository.searchByDoctor(DOCTOR_ID, "Sertraline", null))
                    .thenReturn(List.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            List<PrescriptionResponseDTO> result =
                    prescriptionService.searchPrescriptions("Sertraline", null, DOCTOR_ID, false);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Admin → doit appeler search() sans filtre docteur")
        void searchPrescriptions_asAdmin_noFilterDoctor() {
            when(prescriptionRepository.search("Sertraline", null))
                    .thenReturn(List.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            List<PrescriptionResponseDTO> result =
                    prescriptionService.searchPrescriptions("Sertraline", null, DOCTOR_ID, true);

            assertThat(result).hasSize(1);
            verify(prescriptionRepository).search("Sertraline", null);
            verify(prescriptionRepository, never()).searchByDoctor(anyLong(), anyString(), any());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  KEYWORD COMPLEXE: CRITICAL PRESCRIPTIONS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCriticalPrescriptions()")
    class GetCriticalPrescriptions {

        @Test
        @DisplayName("Admin → doit chercher les prescriptions HIGH du patient")
        void getCriticalPrescriptions_asAdmin_returnsHighSeverity() {
            when(prescriptionRepository.findByPatientIdAndMedicalRecord_Severity(PATIENT_ID, Severity.HIGH))
                    .thenReturn(List.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            List<PrescriptionResponseDTO> result =
                    prescriptionService.getCriticalPrescriptions(PATIENT_ID, DOCTOR_ID, true);

            assertThat(result).hasSize(1);
            verify(prescriptionRepository)
                    .findByPatientIdAndMedicalRecord_Severity(PATIENT_ID, Severity.HIGH);
        }

        @Test
        @DisplayName("Docteur → doit filtrer par doctorId en plus de la sévérité")
        void getCriticalPrescriptions_asDoctor_filtersDoctor() {
            when(prescriptionRepository
                    .findByPatientIdAndMedicalRecord_SeverityAndDoctorId(PATIENT_ID, Severity.HIGH, DOCTOR_ID))
                    .thenReturn(List.of(samplePrescription));
            when(prescriptionMapper.toResponseDTO(samplePrescription)).thenReturn(sampleResponseDTO);

            List<PrescriptionResponseDTO> result =
                    prescriptionService.getCriticalPrescriptions(PATIENT_ID, DOCTOR_ID, false);

            assertThat(result).hasSize(1);
            verify(prescriptionRepository)
                    .findByPatientIdAndMedicalRecord_SeverityAndDoctorId(PATIENT_ID, Severity.HIGH, DOCTOR_ID);
        }
    }
}
