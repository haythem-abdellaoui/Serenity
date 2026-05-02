package serenity.doctors_service.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.DoctorVerification;

@Service
public class RedisPublisher {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    public void publishVerification(DoctorVerification verification) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            String json = mapper.writeValueAsString(verification);
            redisTemplate.convertAndSend("doctor-verifications", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void publishChatMessage(Object message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            String json = mapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat-messages", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void publishApproveContract(Object message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            String json = mapper.writeValueAsString(message);
            redisTemplate.convertAndSend("approve-contract", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}