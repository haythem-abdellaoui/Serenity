package com.example.healthcare.controller;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.dto.DoctorUpdateRequest;
import com.example.healthcare.entity.Doctor;
import com.example.healthcare.service.DoctorService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@Validated
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/{userId}")
    public ResponseEntity<DoctorResponseDTO> createDoctorForExistingUser(
            @PathVariable Long userId,
            @RequestParam("speciality") @NotBlank @Size(min = 2) @Pattern(regexp = "^[A-Za-z ]+$") String speciality,
            @RequestParam("image") @NotNull MultipartFile image
    ) throws IOException {
        DoctorResponseDTO doctor = doctorService.createDoctorForExistingUser(userId, speciality, image);
        return ResponseEntity.ok(doctor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DoctorResponseDTO>> getAllDoctors() {
        List<DoctorResponseDTO> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponseDTO> getDoctorById(@PathVariable Long id) {
        return doctorService.getDoctorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DoctorResponseDTO> updateDoctor(
            @PathVariable Long id,
            @RequestParam(required = false) @Size(min = 2) @Pattern(regexp = "^[A-Za-z ]+$") String specialty,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false)
            @Pattern(regexp = "^$|^\\d{8}$", message = "Phone must be exactly 8 digits") String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateOfBirth,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String preferredLanguage,
            @RequestParam(required = false) Boolean isAnonymous
    ) throws IOException {

        DoctorUpdateRequest request = DoctorUpdateRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .dateOfBirth(dateOfBirth)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .preferredLanguage(preferredLanguage)
                .isAnonymous(isAnonymous)
                .specialty(specialty)
                .image(image)
                .build();

        DoctorResponseDTO updated = doctorService.updateDoctorFull(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("VerifyDoctor/{id}")
    public ResponseEntity<Void> verifyDoctor(@PathVariable Long id) {
        doctorService.Verify(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/email")
    public ResponseEntity<String> getDoctorEmail(@RequestParam Long doctorId) {
        String email = doctorService.getDoctorEmail(doctorId);
        return ResponseEntity.ok(email);
    }
}