package tn.esprit.arctic.derbelmicroservice.service;

import tn.esprit.arctic.derbelmicroservice.dto.request.MedicineRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;

import java.util.List;

public interface IMedicineService {

    List<MedicineResponseDTO> getAllMedicines();

    List<MedicineResponseDTO> searchMedicines(String name);

    MedicineResponseDTO getMedicineById(Long id);

    MedicineResponseDTO createMedicine(MedicineRequestDTO dto);

    MedicineResponseDTO updateMedicine(Long id, MedicineRequestDTO dto);

    void deleteMedicine(Long id);

    List<tn.esprit.arctic.derbelmicroservice.dto.response.OpenFDAMedicineDTO> searchExternalFdaMedicines(String query);

    MedicineResponseDTO getOrCreateMedicine(MedicineRequestDTO dto);
}
