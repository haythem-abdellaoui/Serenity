package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.entity.Doctor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void publishDoctorEvent(DoctorResponseDTO doctor) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Optionnel : formater les dates correctement
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.registerModule(new JavaTimeModule()); // si tu as LocalDate, Date, etc.

            String json = mapper.writeValueAsString(doctor);
            redisTemplate.convertAndSend("doctor-events", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}