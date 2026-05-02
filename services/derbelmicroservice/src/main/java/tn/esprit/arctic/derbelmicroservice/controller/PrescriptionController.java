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
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PageResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;
import tn.esprit.arctic.derbelmicroservice.service.IPdfGeneratorService;
import tn.esprit.arctic.derbelmicroservice.service.IPrescriptionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final IPrescriptionService prescriptionService;
    private final IPdfGeneratorService pdfGeneratorService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PrescriptionResponseDTO>>> getAllPrescriptions(
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

        Page<PrescriptionResponseDTO> resultPage = prescriptionService.getAllPrescriptions(pageable, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PageResponseDTO<PrescriptionResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Liste des prescriptions récupérée avec succès")
                .data(PageResponseDTO.of(resultPage))
                .build());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponseDTO<PrescriptionResponseDTO>> getPrescriptionById(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        PrescriptionResponseDTO prescription = prescriptionService.getPrescriptionById(id, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PrescriptionResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Prescription récupérée avec succès")
                .data(prescription)
                .build());
    }

    @GetMapping("/{id:\\d+}/pdf")
    public ResponseEntity<byte[]> downloadPrescriptionPdf(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        try {
            PrescriptionResponseDTO prescription = prescriptionService.getPrescriptionById(id, authenticatedUserId, isAdmin);
            byte[] pdfBytes = pdfGeneratorService.generatePrescriptionPdf(prescription);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ordonnance-" + id + ".pdf\"");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<ApiResponseDTO<List<PrescriptionResponseDTO>>> getPrescriptionsByRecordId(
            @PathVariable Long recordId) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        List<PrescriptionResponseDTO> prescriptions = prescriptionService.getPrescriptionsByRecordId(recordId, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<List<PrescriptionResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Prescriptions du dossier médical récupérées avec succès")
                .data(prescriptions)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PrescriptionResponseDTO>> createPrescription(
            @Valid @RequestBody PrescriptionRequestDTO requestDTO) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        PrescriptionResponseDTO created = prescriptionService.createPrescription(requestDTO, authenticatedUserId, isAdmin);
        return new ResponseEntity<>(ApiResponseDTO.<PrescriptionResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message("Prescription créée avec succès")
                .data(created)
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponseDTO<PrescriptionResponseDTO>> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRequestDTO requestDTO) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        PrescriptionResponseDTO updated = prescriptionService.updatePrescription(id, requestDTO, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PrescriptionResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Prescription mise à jour avec succès")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePrescription(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        prescriptionService.deletePrescription(id, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Prescription supprimée avec succès")
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<List<PrescriptionResponseDTO>>> searchPrescriptions(
            @RequestParam(required = false) String medicationName,
            @RequestParam(required = false) String status) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        List<PrescriptionResponseDTO> results = prescriptionService.searchPrescriptions(
                medicationName, status, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<List<PrescriptionResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Résultats de la recherche")
                .data(results)
                .build());
    }

    // ── Keywords Complexes: Prescriptions critiques ──
    @GetMapping("/patient/{patientId}/critical")
    public ResponseEntity<ApiResponseDTO<List<PrescriptionResponseDTO>>> getCriticalPrescriptions(
            @PathVariable Long patientId) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        List<PrescriptionResponseDTO> results = prescriptionService.getCriticalPrescriptions(patientId, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<List<PrescriptionResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Prescriptions critiques (Keywords cross-table)")
                .data(results)
                .build());
    }
}
