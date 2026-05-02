package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicineRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.Medicine;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.MedicineMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicineRepository;
import tn.esprit.arctic.derbelmicroservice.service.IMedicineService;
import tn.esprit.arctic.derbelmicroservice.dto.response.OpenFDAMedicineDTO;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicineServiceImpl implements IMedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineMapper medicineMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDTO> getAllMedicines() {
        return medicineRepository.findAll().stream()
                .map(medicineMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDTO> searchMedicines(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name).stream()
                .map(medicineMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicineResponseDTO getMedicineById(Long id) {
        return medicineMapper.toResponseDTO(findOrThrow(id));
    }

    @Override
    public MedicineResponseDTO createMedicine(MedicineRequestDTO dto) {
        if (medicineRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new IllegalArgumentException("Un médicament avec ce nom existe déjà: " + dto.getName());
        }
        Medicine entity = medicineMapper.toEntity(dto);
        return medicineMapper.toResponseDTO(medicineRepository.save(entity));
    }

    @Override
    public MedicineResponseDTO updateMedicine(Long id, MedicineRequestDTO dto) {
        Medicine entity = findOrThrow(id);
        medicineRepository.findByNameIgnoreCase(dto.getName().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Un médicament avec ce nom existe déjà: " + dto.getName());
                });
        medicineMapper.updateEntity(dto, entity);
        return medicineMapper.toResponseDTO(medicineRepository.save(entity));
    }

    @Override
    public void deleteMedicine(Long id) {
        Medicine entity = findOrThrow(id);
        medicineRepository.delete(entity);
    }

    @Override
    public List<OpenFDAMedicineDTO> searchExternalFdaMedicines(String query) {
        List<OpenFDAMedicineDTO> results = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return results;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromUriString("https://api.fda.gov/drug/label.json")
                    .queryParam("search", "openfda.brand_name:*" + query + "*+openfda.generic_name:*" + query + "*")
                    .queryParam("limit", 10)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response);
                JsonNode resultsNode = rootNode.path("results");
                if (resultsNode.isArray()) {
                    for (JsonNode node : resultsNode) {
                        String name = query;
                        JsonNode openfda = node.path("openfda");
                        if (!openfda.isMissingNode()) {
                            if (openfda.has("brand_name") && openfda.get("brand_name").isArray()) {
                                name = openfda.get("brand_name").get(0).asText();
                            } else if (openfda.has("generic_name") && openfda.get("generic_name").isArray()) {
                                name = openfda.get("generic_name").get(0).asText();
                            }
                        }

                        String description = "";
                        if (node.has("indications_and_usage") && node.get("indications_and_usage").isArray()) {
                            description = node.get("indications_and_usage").get(0).asText();
                        }

                        String sideEffects = "";
                        if (node.has("adverse_reactions") && node.get("adverse_reactions").isArray()) {
                            sideEffects = node.get("adverse_reactions").get(0).asText();
                        } else if (node.has("warnings") && node.get("warnings").isArray()) {
                            sideEffects = node.get("warnings").get(0).asText();
                        }

                        results.add(OpenFDAMedicineDTO.builder()
                                .name(name)
                                .description(description.length() > 500 ? description.substring(0, 497) + "..." : description)
                                .sideEffects(sideEffects.length() > 500 ? sideEffects.substring(0, 497) + "..." : sideEffects)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling OpenFDA API: " + e.getMessage());
        }
        return results;
    }

    @Override
    public MedicineResponseDTO getOrCreateMedicine(MedicineRequestDTO dto) {
        Optional<Medicine> existing = medicineRepository.findByNameIgnoreCase(dto.getName().trim());
        if (existing.isPresent()) {
            return medicineMapper.toResponseDTO(existing.get());
        }
        Medicine entity = medicineMapper.toEntity(dto);
        return medicineMapper.toResponseDTO(medicineRepository.save(entity));
    }

    private Medicine findOrThrow(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", "id", id));
    }
}
