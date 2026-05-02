package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisPublisherTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @InjectMocks private RedisPublisher publisher;

    @Test
    void publishDoctorEvent_sendsJsonToChannel() {
        DoctorResponseDTO dto = DoctorResponseDTO.builder()
                .id(1L)
                .email("doc@example.com")
                .build();

        publisher.publishDoctorEvent(dto);

        verify(redisTemplate).convertAndSend(eq("doctor-events"), startsWith("{"));
    }
}

