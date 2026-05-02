package serenity.doctors_service.service;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IDoctorVerificationService {
    DoctorVerification save(DoctorVerification verification);

    DoctorVerification saveVerification(Long doctorId, MultipartFile cv, MultipartFile diploma,
                                        String licenseNumber, String nationalId) throws IOException;

    List<DoctorVerification> findAll();

    List<DoctorVerification> findById(Long verification_id);

    List<DoctorVerification> findByDoctorId(Long id);

    void deleteById(Long verification_id);

    void Approve(Long verification_id, @RequestHeader("Authorization") String authHeader);

    void Reject(Long verification_id, @RequestHeader("Authorization") String authHeader);

    void testEmail();

    void approveContract(String token);

    List<DoctorVerification> getRejected();
}
