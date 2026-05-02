package serenity.doctors_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.repository.DoctorVerificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorVerificationServiceTest {

    @Mock
    private DoctorVerificationRepository repository;

    @Mock
    private MailService mailService;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisPublisher publisher;

    @InjectMocks
    private DoctorVerificationService service;

    @Test
    void save_shouldReturnEntity() {
        DoctorVerification v = new DoctorVerification();
        when(repository.save(any())).thenReturn(v);

        DoctorVerification result = service.save(v);

        assertNotNull(result);
        verify(repository).save(v);
    }

    @Test
    void saveVerification_shouldSaveFilesAndPublishVerification() throws Exception {
        MockMultipartFile cv = new MockMultipartFile(
                "cv",
                "cv.pdf",
                "application/pdf",
                "cv-content".getBytes()
        );

        MockMultipartFile diploma = new MockMultipartFile(
                "diploma",
                "diploma.pdf",
                "application/pdf",
                "diploma-content".getBytes()
        );

        when(repository.save(any(DoctorVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DoctorVerification result = service.saveVerification(
                1L,
                cv,
                diploma,
                "LIC-123",
                "NID-456"
        );

        assertNotNull(result);
        assertEquals(2L, result.getDoctorId());
        assertEquals("LIC-123", result.getLicenseNumber());
        assertEquals("NID-456", result.getNationalId());
        assertEquals(DoctorVerification.Status.PENDING, result.getStatus());
        assertNotNull(result.getSubmittedAt());
        assertNotNull(result.getCV());
        assertNotNull(result.getDiploma());
        assertTrue(result.getCV().contains("uploads"));
        assertTrue(result.getDiploma().contains("uploads"));

        verify(repository).save(any(DoctorVerification.class));
        verify(redisPublisher, never()).publishApproveContract(any());
        verifyNoMoreInteractions(mailService);
    }

    @Test
    void findAll_shouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(new DoctorVerification()));

        List<DoctorVerification> result = service.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void findById_shouldReturnList() {
        DoctorVerification v = new DoctorVerification();
        when(repository.findById(1L)).thenReturn(Optional.of(v));

        List<DoctorVerification> result = service.findById(1L);

        assertEquals(1, result.size());
    }

    @Test
    void findById_shouldReturnEmpty() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        List<DoctorVerification> result = service.findById(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByDoctorId_shouldReturnList() {
        when(repository.findByDoctorId(1L)).thenReturn(List.of(new DoctorVerification()));

        List<DoctorVerification> result = service.findByDoctorId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void delete_shouldCallRepo() {
        service.deleteById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void approve_shouldSendEmailAndSaveToken() {
        DoctorVerification v = new DoctorVerification();
        v.setDoctorId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(v));
        when(repository.save(any())).thenReturn(v);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("test@mail.com"));

        service.Approve(1L, "Bearer token");

        assertNotNull(v.getApprovalToken());
        verify(mailService).sendEmail(eq("test@mail.com"), eq("Verification Approved – Serenity"), contains("contrat?token="));
        verify(repository, atLeastOnce()).save(any());
    }

    @Test
    void reject_shouldUpdateAndSendEmail() {
        DoctorVerification v = new DoctorVerification();
        v.setDoctorId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(v));
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("test@mail.com"));

        service.Reject(1L, "Bearer token");

        assertEquals(DoctorVerification.Status.REJECTED, v.getStatus());
        assertNotNull(v.getRejectionDate());
        verify(mailService).sendEmail(eq("test@mail.com"), eq("Verification Rejected – Serenity"), contains("regret"));
        verify(repository).save(v);
    }

    @Test
    void testEmail_shouldSendEmail() {
        service.testEmail();
        verify(mailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void approveContract_shouldActivateContract() {
        DoctorVerification v = new DoctorVerification();
        v.setApprovalToken("token");

        when(repository.findByApprovalToken("token"))
                .thenReturn(Optional.of(v));

        service.approveContract("token");

        assertTrue(v.isContractApproved());
        assertNull(v.getApprovalToken());
        verify(repository).save(v);
        verify(redisPublisher).publishApproveContract(v);
    }

    @Test
    void approveContract_shouldThrowException() {
        when(repository.findByApprovalToken("bad"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.approveContract("bad"));
    }

    @Test
    void getRejected_shouldReturnList() {
        when(repository.findByStatusAndRejectionDateIsNotNullOrderByRejectionDateDesc(
                DoctorVerification.Status.REJECTED))
                .thenReturn(List.of(new DoctorVerification()));

        List<DoctorVerification> result = service.getRejected();

        assertEquals(1, result.size());
    }

    @Test
    void cleanRejected_shouldNotDeleteWhenNotEligible() {
        DoctorVerification v = new DoctorVerification();
        v.setVerification_id(10L);
        v.setStatus(DoctorVerification.Status.APPROVED);
        v.setRejectionDate(LocalDateTime.now().minusDays(8));

        when(repository.findAll()).thenReturn(List.of(v));

        service.cleanRejected();

        verify(repository, never()).deleteById(any());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(Void.class));
    }

    @Test
    void cleanRejected_shouldDeleteExpiredRejectedVerificationEvenIfDoctorDeleteFails() {
        DoctorVerification v = new DoctorVerification();
        v.setVerification_id(20L);
        v.setDoctorId(30L);
        v.setStatus(DoctorVerification.Status.REJECTED);
        v.setRejectionDate(LocalDateTime.now().minusDays(8));

        when(repository.findAll()).thenReturn(List.of(v));
        doThrow(new RuntimeException("boom"))
                .when(restTemplate)
                .exchange(anyString(), any(), any(), eq(Void.class));

        assertDoesNotThrow(() -> service.cleanRejected());

        verify(repository).deleteById(20L);
        verify(restTemplate).exchange(contains("http://localhost:8081/api/doctors/30"), any(), any(), eq(Void.class));
    }
}
