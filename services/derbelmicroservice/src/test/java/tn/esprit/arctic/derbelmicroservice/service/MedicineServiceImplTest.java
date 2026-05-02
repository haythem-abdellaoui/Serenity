package tn.esprit.arctic.derbelmicroservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicineRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.Medicine;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.MedicineMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicineRepository;
import tn.esprit.arctic.derbelmicroservice.service.impl.MedicineServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour MedicineServiceImpl.
 * Couvre le CRUD, la vérification d'unicité du nom,
 * la méthode getOrCreate, et la recherche.
 */
@ExtendWith(MockitoExtension.class)
class MedicineServiceImplTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineMapper medicineMapper;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    private Medicine sampleMedicine;
    private MedicineResponseDTO sampleResponseDTO;
    private MedicineRequestDTO sampleRequestDTO;

    @BeforeEach
    void setUp() {
        sampleMedicine = Medicine.builder()
                .id(1L)
                .name("Sertraline")
                .description("Antidépresseur ISRS")
                .sideEffects("Nausées, insomnie")
                .build();

        sampleResponseDTO = MedicineResponseDTO.builder()
                .id(1L)
                .name("Sertraline")
                .description("Antidépresseur ISRS")
                .sideEffects("Nausées, insomnie")
                .build();

        sampleRequestDTO = MedicineRequestDTO.builder()
                .name("Sertraline")
                .description("Antidépresseur ISRS")
                .sideEffects("Nausées, insomnie")
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  GET ALL MEDICINES
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllMedicines()")
    class GetAllMedicines {

        @Test
        @DisplayName("Doit retourner la liste complète des médicaments")
        void getAllMedicines_returnsList() {
            when(medicineRepository.findAll()).thenReturn(List.of(sampleMedicine));
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            List<MedicineResponseDTO> result = medicineService.getAllMedicines();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Sertraline");
        }

        @Test
        @DisplayName("Aucun médicament → doit retourner une liste vide")
        void getAllMedicines_empty_returnsEmptyList() {
            when(medicineRepository.findAll()).thenReturn(List.of());

            List<MedicineResponseDTO> result = medicineService.getAllMedicines();

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SEARCH MEDICINES
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchMedicines()")
    class SearchMedicines {

        @Test
        @DisplayName("Recherche 'sert' → doit retourner les médicaments correspondants")
        void searchMedicines_partialName_returnsMatches() {
            when(medicineRepository.findByNameContainingIgnoreCase("sert"))
                    .thenReturn(List.of(sampleMedicine));
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            List<MedicineResponseDTO> result = medicineService.searchMedicines("sert");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Sertraline");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET BY ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMedicineById()")
    class GetMedicineById {

        @Test
        @DisplayName("ID existant → doit retourner le médicament")
        void getMedicineById_existingId_returnsMedicine() {
            when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            MedicineResponseDTO result = medicineService.getMedicineById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Sertraline");
        }

        @Test
        @DisplayName("ID inexistant → doit lever ResourceNotFoundException")
        void getMedicineById_notFound_throwsException() {
            when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> medicineService.getMedicineById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Medicine");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CREATE MEDICINE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createMedicine()")
    class CreateMedicine {

        @Test
        @DisplayName("Nom unique → doit créer et retourner le médicament")
        void createMedicine_uniqueName_createsSuccessfully() {
            when(medicineRepository.existsByNameIgnoreCase("Sertraline")).thenReturn(false);
            when(medicineMapper.toEntity(sampleRequestDTO)).thenReturn(sampleMedicine);
            when(medicineRepository.save(sampleMedicine)).thenReturn(sampleMedicine);
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            MedicineResponseDTO result = medicineService.createMedicine(sampleRequestDTO);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Sertraline");
            verify(medicineRepository).save(sampleMedicine);
        }

        @Test
        @DisplayName("Nom déjà existant → doit lever IllegalArgumentException")
        void createMedicine_duplicateName_throwsException() {
            when(medicineRepository.existsByNameIgnoreCase("Sertraline")).thenReturn(true);

            assertThatThrownBy(() -> medicineService.createMedicine(sampleRequestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existe déjà");

            verify(medicineRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UPDATE MEDICINE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateMedicine()")
    class UpdateMedicine {

        @Test
        @DisplayName("ID existant + nom unique → doit mettre à jour avec succès")
        void updateMedicine_validUpdate_updatesSuccessfully() {
            when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
            when(medicineRepository.findByNameIgnoreCase("Sertraline")).thenReturn(Optional.of(sampleMedicine));
            when(medicineRepository.save(sampleMedicine)).thenReturn(sampleMedicine);
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            MedicineResponseDTO result = medicineService.updateMedicine(1L, sampleRequestDTO);

            assertThat(result).isNotNull();
            verify(medicineMapper).updateEntity(sampleRequestDTO, sampleMedicine);
            verify(medicineRepository).save(sampleMedicine);
        }

        @Test
        @DisplayName("Changement de nom vers un nom déjà pris → doit lever IllegalArgumentException")
        void updateMedicine_duplicateName_throwsException() {
            Medicine otherMedicine = Medicine.builder().id(2L).name("Sertraline").build();

            when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
            when(medicineRepository.findByNameIgnoreCase("Sertraline")).thenReturn(Optional.of(otherMedicine));

            assertThatThrownBy(() -> medicineService.updateMedicine(1L, sampleRequestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existe déjà");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DELETE MEDICINE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteMedicine()")
    class DeleteMedicine {

        @Test
        @DisplayName("ID existant → doit supprimer avec succès")
        void deleteMedicine_existingId_deletesSuccessfully() {
            when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));

            medicineService.deleteMedicine(1L);

            verify(medicineRepository).delete(sampleMedicine);
        }

        @Test
        @DisplayName("ID inexistant → doit lever ResourceNotFoundException")
        void deleteMedicine_notFound_throwsException() {
            when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> medicineService.deleteMedicine(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET OR CREATE MEDICINE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getOrCreateMedicine()")
    class GetOrCreateMedicine {

        @Test
        @DisplayName("Médicament existant → doit retourner l'existant SANS créer")
        void getOrCreateMedicine_existing_returnsExisting() {
            when(medicineRepository.findByNameIgnoreCase("Sertraline"))
                    .thenReturn(Optional.of(sampleMedicine));
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            MedicineResponseDTO result = medicineService.getOrCreateMedicine(sampleRequestDTO);

            assertThat(result.getName()).isEqualTo("Sertraline");
            verify(medicineRepository, never()).save(any());
        }

        @Test
        @DisplayName("Médicament inexistant → doit le créer et le retourner")
        void getOrCreateMedicine_notExisting_createsNew() {
            when(medicineRepository.findByNameIgnoreCase("Sertraline"))
                    .thenReturn(Optional.empty());
            when(medicineMapper.toEntity(sampleRequestDTO)).thenReturn(sampleMedicine);
            when(medicineRepository.save(sampleMedicine)).thenReturn(sampleMedicine);
            when(medicineMapper.toResponseDTO(sampleMedicine)).thenReturn(sampleResponseDTO);

            MedicineResponseDTO result = medicineService.getOrCreateMedicine(sampleRequestDTO);

            assertThat(result.getName()).isEqualTo("Sertraline");
            verify(medicineRepository).save(sampleMedicine);
        }
    }
}
