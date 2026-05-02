package serenity.doctors_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import serenity.doctors_service.entity.DoctorVerification;

import java.util.List;
import java.util.Optional;

public interface DoctorVerificationRepository  extends JpaRepository<DoctorVerification, Long> {
    List<DoctorVerification> findByDoctorId(Long id);

    Optional<DoctorVerification> findByApprovalToken(String token);

    List<DoctorVerification> findByStatusAndRejectionDateIsNotNullOrderByRejectionDateDesc(
            DoctorVerification.Status status
    );
}
