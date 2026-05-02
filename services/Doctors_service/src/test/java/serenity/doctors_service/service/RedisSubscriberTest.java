package serenity.doctors_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import serenity.doctors_service.entity.DoctorVerification;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisSubscriberTest {

    @Test
    void receiveMessage_shouldSendDoctorVerification() throws Exception {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        DoctorVerification verification = new DoctorVerification();
        String json = new ObjectMapper().writeValueAsString(verification);

        redisSubscriber.receiveMessage(json);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/doctor-verifications"), captor.capture());
        assertNotNull(captor.getValue());
        assertInstanceOf(DoctorVerification.class, captor.getValue());
    }

    @Test
    void receiveMessage_shouldHandleInvalidJson() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        redisSubscriber.receiveMessage("invalid-json");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void receiveChatMessage_shouldSendPayload() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        String json = "{\"message\":\"hello\",\"user\":\"test\"}";

        redisSubscriber.receiveChatMessage(json);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat-messages"), captor.capture());
        assertInstanceOf(Map.class, captor.getValue());
    }

    @Test
    void receiveChatMessage_shouldHandleInvalidJson() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        redisSubscriber.receiveChatMessage("bad json");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void receiveChatMessage_shouldHandleUnexpectedException() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        String json = "{\"message\":\"hello\"}";

        doThrow(new RuntimeException("ws send failed"))
                .when(messagingTemplate)
                .convertAndSend(eq("/topic/chat-messages"), any(Object.class));

        assertDoesNotThrow(() -> redisSubscriber.receiveChatMessage(json));

        verify(messagingTemplate).convertAndSend(eq("/topic/chat-messages"), any(Object.class));
    }

    @Test
    void receiveApproveContract_shouldSendPayload() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        String json = "{\"contractId\":1,\"status\":\"APPROVED\"}";

        redisSubscriber.receiveApproveContract(json);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/approve-contract"), captor.capture());
        assertInstanceOf(Map.class, captor.getValue());
    }

    @Test
    void receiveApproveContract_shouldHandleInvalidJson() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber redisSubscriber = new RedisSubscriber(messagingTemplate);

        redisSubscriber.receiveApproveContract("bad json");

        verifyNoInteractions(messagingTemplate);
    }
}
