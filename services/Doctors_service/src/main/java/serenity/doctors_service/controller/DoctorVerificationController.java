package serenity.doctors_service.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.security.JwtService;
import serenity.doctors_service.service.IDoctorVerificationService;
import serenity.doctors_service.service.RedisPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctor-verifications")
public class DoctorVerificationController {

    @Autowired
    private IDoctorVerificationService service;
    private final String uploadDir = "uploads/";

    @Autowired
    private RedisPublisher redisPublisher;

    @Autowired
    private JwtService jwtService;

    // Create verification with files
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/add_verification")
    public ResponseEntity<DoctorVerification> create(
            @RequestParam("cv") @NotNull MultipartFile cv,
            @RequestParam("diploma") @NotNull MultipartFile diploma,
            @RequestParam("licenseNumber") @NotBlank @Size(min = 5, max = 20) @Pattern(regexp = "^[A-Za-z0-9 ]+$") String licenseNumber,
            @RequestParam("nationalId") @NotBlank @Size(min = 8, max = 8) @Pattern(regexp = "^[0-9 ]+$") String nationalId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "x-doctor-id", required = false) String doctorIdHeader,
            @RequestHeader(value = "userid", required = false) String userIdFallback,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) throws IOException {

        Long doctorId = resolveDoctorId(userIdHeader, doctorIdHeader, userIdFallback, authorization);

        DoctorVerification verification = service.saveVerification(
                doctorId, cv, diploma, licenseNumber, nationalId
        );

        return ResponseEntity.ok(verification);
    }

    // Update verification
    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/update_verification/{id}")
    public ResponseEntity<DoctorVerification> update(
            @PathVariable Long id,
            @RequestParam(value = "cv", required = false) MultipartFile cv,
            @RequestParam(value = "diploma", required = false) MultipartFile diploma,
            @RequestParam("licenseNumber") @NotBlank @Size(min = 5, max = 20) @Pattern(regexp = "^[A-Za-z0-9 ]+$") String licenseNumber,
            @RequestParam("nationalId") @NotBlank @Size(min = 8, max = 8) @Pattern(regexp = "^[0-9 ]+$") String nationalId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "x-doctor-id", required = false) String doctorIdHeader,
            @RequestHeader(value = "userid", required = false) String userIdFallback,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) throws IOException {

        Long doctorId = resolveDoctorId(userIdHeader, doctorIdHeader, userIdFallback, authorization);

        List<DoctorVerification> list = service.findById(id);
        if (list.isEmpty()) {
            throw new RuntimeException("Verification not found");
        }
        DoctorVerification verification = list.get(0);

        verification.setLicenseNumber(licenseNumber);
        verification.setNationalId(nationalId);

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        if (cv != null) {
            String cvFileName = System.currentTimeMillis() + "_" + cv.getOriginalFilename();
            Path cvPath = uploadPath.resolve(cvFileName);
            Files.copy(cv.getInputStream(), cvPath, StandardCopyOption.REPLACE_EXISTING);
            verification.setCV(cvPath.toString());
        }

        if (diploma != null) {
            String diplomaFileName = System.currentTimeMillis() + "_" + diploma.getOriginalFilename();
            Path diplomaPath = uploadPath.resolve(diplomaFileName);
            Files.copy(diploma.getInputStream(), diplomaPath, StandardCopyOption.REPLACE_EXISTING);
            verification.setDiploma(diplomaPath.toString());
        }

        DoctorVerification updated = service.save(verification);
        redisPublisher.publishVerification(updated);

        return ResponseEntity.ok(updated);
    }

    // Get all verifications
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DoctorVerification>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // Get verification by ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<List<DoctorVerification>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @GetMapping("FindByDoctorID/{id}")
    public ResponseEntity<List<DoctorVerification>> findByDoctorID(@PathVariable Long id) {
        return ResponseEntity.ok(service.findByDoctorId(id));
    }

    // Delete verification by ID
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("Approve/{id}")
    public ResponseEntity<Void> approve(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        service.Approve(id, authHeader);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("Reject/{id}")
    public ResponseEntity<Void> reject(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        service.Reject(id, authHeader);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test-email")
    public ResponseEntity<Void> testEmail() {
        service.testEmail();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/approve-contract")
    public ResponseEntity<Void> approveContract(@RequestParam("token") String token) {
        service.approveContract(token);
        return ResponseEntity.ok().build();
    }

    private Long resolveDoctorId(String xUserId, String xDoctorId, String userId, String authorization) {
        String raw = firstNonBlank(xUserId, xDoctorId, userId);
        if (StringUtils.hasText(raw)) {
            return Long.parseLong(raw.trim());
        }

        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            Long tokenUserId = jwtService.extractUserId(authorization.substring(7));
            if (tokenUserId != null) {
                return tokenUserId;
            }
        }

        throw new IllegalArgumentException("Missing doctor identifier");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rejected-keywords")
    public List<DoctorVerification> getRejectedKeywords() {
        return service.getRejected();
    }
}