package tn.esprit.arctic.derbelmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PageResponseDTO;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;
import tn.esprit.arctic.derbelmicroservice.service.IMedicalRecordService;

import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final IMedicalRecordService medicalRecordService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<MedicalRecordResponseDTO>>> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MedicalRecordResponseDTO> resultPage = medicalRecordService.getAllRecords(pageable, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PageResponseDTO<MedicalRecordResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Liste des dossiers médicaux récupérée avec succès")
                .data(PageResponseDTO.of(resultPage))
                .build());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponseDTO<MedicalRecordResponseDTO>> getRecordById(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        MedicalRecordResponseDTO record = medicalRecordService.getRecordById(id, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<MedicalRecordResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Dossier médical récupéré avec succès")
                .data(record)
                .build());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponseDTO<List<MedicalRecordResponseDTO>>> getRecordsByPatientId(
            @PathVariable Long patientId) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        List<MedicalRecordResponseDTO> records = medicalRecordService.getRecordsByPatientId(patientId, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<List<MedicalRecordResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Dossiers médicaux du patient récupérés avec succès")
                .data(records)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<MedicalRecordResponseDTO>> createRecord(
            @Valid @RequestBody MedicalRecordRequestDTO requestDTO) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        MedicalRecordResponseDTO created = medicalRecordService.createRecord(requestDTO, authenticatedUserId, isAdmin);
        return new ResponseEntity<>(ApiResponseDTO.<MedicalRecordResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message("Dossier médical créé avec succès")
                .data(created)
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponseDTO<MedicalRecordResponseDTO>> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody MedicalRecordRequestDTO requestDTO) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        MedicalRecordResponseDTO updated = medicalRecordService.updateRecord(id, requestDTO, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<MedicalRecordResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Dossier médical mis à jour avec succès")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteRecord(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        medicalRecordService.deleteRecord(id, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Dossier médical supprimé avec succès")
                .build());
    }

    @GetMapping("/patient/{patientId}/active-treatments")
    public ResponseEntity<ApiResponseDTO<List<MedicalRecordResponseDTO>>> getRecordsWithActiveTreatment(
            @PathVariable Long patientId) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        List<MedicalRecordResponseDTO> records = medicalRecordService.getRecordsWithActiveTreatment(patientId, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<List<MedicalRecordResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Dossiers avec traitement actif (JPQL JOIN)")
                .data(records)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<List<MedicalRecordResponseDTO>>> searchRecords(
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        List<MedicalRecordResponseDTO> results = medicalRecordService.searchRecords(
                diagnosis, status, severity, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<List<MedicalRecordResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Résultats de la recherche")
                .data(results)
                .build());
    }
}
