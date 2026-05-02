package serenity.doctors_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import serenity.doctors_service.entity.DoctorVerification;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private RedisPublisher redisPublisher;

    @Test
    void publishVerification_shouldSendToRedis() {
        DoctorVerification v = new DoctorVerification();

        redisPublisher.publishVerification(v);

        verify(redisTemplate).convertAndSend(eq("doctor-verifications"), anyString());
    }

    @Test
    void publishChatMessage_shouldSendToRedis() {
        redisPublisher.publishChatMessage("test-message");

        verify(redisTemplate).convertAndSend(eq("chat-messages"), anyString());
    }

    @Test
    void publishApproveContract_shouldSendToRedis() {
        redisPublisher.publishApproveContract("contract");

        verify(redisTemplate).convertAndSend(eq("approve-contract"), anyString());
    }

    @Test
    void publishVerification_shouldHandleSerializationException() {
        DoctorVerification bad = new DoctorVerification() {
            @Override
            public Long getDoctorId() {
                throw new RuntimeException("boom");
            }
        };

        redisPublisher.publishVerification(bad);

        verify(redisTemplate, never()).convertAndSend(eq("doctor-verifications"), anyString());
    }

    @Test
    void publishChatMessage_shouldHandleSerializationException() {
        redisPublisher.publishChatMessage(new Object());

        verify(redisTemplate, never()).convertAndSend(eq("chat-messages"), anyString());
    }

    @Test
    void publishApproveContract_shouldHandleSerializationException() {
        redisPublisher.publishApproveContract(new Object());

        verify(redisTemplate, never()).convertAndSend(eq("approve-contract"), anyString());
    }
}
