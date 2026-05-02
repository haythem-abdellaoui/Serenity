package serenity.doctors_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.repository.DoctorVerificationRepository;
import serenity.doctors_service.service.RedisPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorVerificationService implements IDoctorVerificationService {

    @Autowired
    private DoctorVerificationRepository repository;
    private final String uploadDir = "uploads/";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MailService mailService;



    @Autowired
    private RedisPublisher publisher;
    @Autowired
    private RedisPublisher redisPublisher;

    @Value("${SCHEDULER_JWT}")
    private String schedulerJwt;

    @Override
    public DoctorVerification save(DoctorVerification verification) {
        return repository.save(verification);
    }

    @Override
    public DoctorVerification saveVerification(Long doctorId, MultipartFile cv, MultipartFile diploma,
                                               String licenseNumber, String nationalId) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String cvFileName = System.currentTimeMillis() + "_" + cv.getOriginalFilename();
        String diplomaFileName = System.currentTimeMillis() + "_" + diploma.getOriginalFilename();

        Path cvPath = uploadPath.resolve(cvFileName);
        Path diplomaPath = uploadPath.resolve(diplomaFileName);

        Files.copy(cv.getInputStream(), cvPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(diploma.getInputStream(), diplomaPath, StandardCopyOption.REPLACE_EXISTING);

        DoctorVerification verification = new DoctorVerification();
        verification.setDoctorId(doctorId + 1L);
        verification.setCV(cvPath.toString());
        verification.setDiploma(diplomaPath.toString());
        verification.setLicenseNumber(licenseNumber);
        verification.setNationalId(nationalId);
        verification.setStatus(DoctorVerification.Status.PENDING);
        verification.setSubmittedAt(LocalDateTime.now());

        System.out.println("UPLOAD PATH = " + uploadPath);

        DoctorVerification savedVerification = repository.save(verification);

        publisher.publishVerification(savedVerification);

        return savedVerification;
    }


    @Override
    public List<DoctorVerification> findAll() {
        return repository.findAll();
    }


    @Override
    public List<DoctorVerification> findById(Long verification_id) {
        return repository.findById(verification_id)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<DoctorVerification> findByDoctorId(Long id) {
        return repository.findByDoctorId(id);
    }


    @Override
    public void deleteById(Long verification_id) {
        repository.deleteById(verification_id);
    }

    @Override
    public void Approve(Long verification_id, @RequestHeader("Authorization") String authHeader){
        DoctorVerification verification = repository.findById(verification_id).get();

        // Generate a random token and save it in the verification
        String token = UUID.randomUUID().toString();
        verification.setApprovalToken(token);
        repository.save(verification);

        Long doctor_id = verification.getDoctorId();
        String url = "http://localhost:8081/api/doctors/email?doctorId=" + doctor_id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        String email = response.getBody();
        String subject = "Verification Approved – Serenity";
        String link = "http://localhost:4200/contrat?token=" + token;

        String message = "<p>Dear Doctor,</p>"
                + "<p>We are pleased to inform you that your verification with <strong>Serenity</strong> has been successfully approved.</p>"
                + "<p>You can access your contract by clicking the link below:</p>"
                + "<p><a href='" + link + "'>View Contract</a></p>"
                + "<p>Thank you for being part of Serenity.</p>"
                + "<p>Best regards,<br>Serenity Team</p>";

        mailService.sendEmail(email, subject, message);
    }

    @Override
    public void Reject(Long verification_id, @RequestHeader("Authorization") String authHeader){
        DoctorVerification verification = repository.findById(verification_id).get();
        verification.setStatus(DoctorVerification.Status.REJECTED);
        verification.setRejectionDate(LocalDateTime.now());


        Long doctor_id = verification.getDoctorId();
        String url = "http://localhost:8081/api/doctors/email?doctorId=" + doctor_id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        String email = response.getBody();
        String subject = "Verification Rejected – Serenity";

        String message = "<p>Dear Doctor,</p>"
                + "<p>We regret to inform you that your verification request with <strong>Serenity</strong> has not been approved at this time.</p>"
                + "<p>You are welcome to submit a new application within the next 7 days. Please note that if no action is taken within this period, your account will be removed from our system.</p>"
                + "<p>We appreciate your interest in joining our platform and encourage you to review your information before reapplying.</p>"
                + "<p>Best regards,<br>Serenity Team</p>";

        mailService.sendEmail(email, subject, message);

        repository.save(verification);
    }

    @Override
    public void testEmail(){
        String recipient = "sihaythemabdellaoui@gmail.com";
        String subject = "Verification Approved – Serenity";
        String message = "<p>Dear Doctor,</p>"
                + "<p>We are pleased to inform you that your verification with <strong>Serenity</strong> has been successfully approved.</p>"
                + "<p>You can access your contract by clicking the link below:</p>"
                + "<p><a href='http://localhost:4200/contrat'>View Contract</a></p>"
                + "<p>Thank you for being part of Serenity.</p>"
                + "<p>Best regards,<br>Serenity Team</p>";

        mailService.sendEmail(recipient, subject, message);
    }

    @Override
    public void approveContract(String token) {
        DoctorVerification verification = repository.findByApprovalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        verification.setContractApproved(true);
        verification.setApprovalToken(null); // invalidate token after use
        repository.save(verification);

        redisPublisher.publishApproveContract(verification);
    }

    @Scheduled(fixedRate = 3600000) // chaque heure
    //@Scheduled(fixedRate = 10000)
    public void cleanRejected() {

        System.out.println("Scheduler running...");

        List<DoctorVerification> list = repository.findAll();

        for (DoctorVerification v : list) {

            if (v.getStatus() == DoctorVerification.Status.REJECTED &&
                    v.getRejectionDate() != null && v.getRejectionDate().plusDays(7).isBefore(LocalDateTime.now())) {

                Long doctorId = v.getDoctorId();

                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + schedulerJwt);

                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    restTemplate.exchange(
                            "http://localhost:8081/api/doctors/" + doctorId,
                            HttpMethod.DELETE,
                            entity,
                            Void.class
                    );

                } catch (Exception e) {
                    System.out.println("Doctor delete failed: " + e.getMessage());
                }

                repository.deleteById(v.getVerification_id());
            }
        }
    }

    @Override
    public List<DoctorVerification> getRejected() {
        return repository.findByStatusAndRejectionDateIsNotNullOrderByRejectionDateDesc(
                DoctorVerification.Status.REJECTED
        );
    }


}
