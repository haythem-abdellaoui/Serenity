package serenity.doctors_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_verification")
public class DoctorVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long verification_id;

    private Long doctorId;

    private String licenseNumber;

    private String nationalId;

    private String CV;

    private String diploma;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime submittedAt;

    private LocalDateTime verifiedAt;

    private Long verifiedBy;

    private String rejectionReason;

    private String approvalToken;

    private LocalDateTime rejectionDate;

    private boolean contractApproved;


    public boolean isContractApproved() {
        return contractApproved;
    }

    public void setContractApproved(boolean contractApproved) {
        this.contractApproved = contractApproved;
    }


    public String getApprovalToken() { return approvalToken; }
    public void setApprovalToken(String approvalToken) { this.approvalToken = approvalToken; }


    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public Long getVerification_id() {
        return verification_id;
    }

    public void setVerification_id(Long verification_id) {
        this.verification_id = verification_id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getDiploma() {
        return diploma;
    }

    public void setDiploma(String diploma) {
        this.diploma = diploma;
    }

    public String getCV() {
        return CV;
    }

    public void setCV(String CV) {
        this.CV = CV;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getRejectionDate() {
        return rejectionDate;
    }

    public void setRejectionDate(LocalDateTime rejectionDate) {
        this.rejectionDate = rejectionDate;
    }
}