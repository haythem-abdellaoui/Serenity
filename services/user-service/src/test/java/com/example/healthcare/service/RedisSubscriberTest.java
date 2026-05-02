package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private RedisSubscriber subscriber;

    @Test
    void receiveMessage_parsesDoctorAndPublishesToTopic() throws JsonProcessingException {
        String json = "{\"id\":1,\"email\":\"doc@example.com\"}";

        subscriber.receiveMessage(json);

        ArgumentCaptor<Doctor> doctorCaptor = ArgumentCaptor.forClass(Doctor.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/doctors"), doctorCaptor.capture());
        assertThat(doctorCaptor.getValue().getEmail()).isEqualTo("doc@example.com");
    }
}

