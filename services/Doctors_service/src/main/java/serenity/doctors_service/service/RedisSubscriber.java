package serenity.doctors_service.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.DoctorVerification;

import java.util.Map;

@Component
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void receiveMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            DoctorVerification verification = mapper.readValue(message, DoctorVerification.class);
            messagingTemplate.convertAndSend("/topic/doctor-verifications", verification);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void receiveChatMessage(String message) {
        try {
            System.out.println("📨 [WS] Raw message reçu depuis Redis: " + message);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            Map<String, Object> payload = mapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            System.out.println("✅ [WS] Payload désérialisé: " + payload);

            messagingTemplate.convertAndSend("/topic/chat-messages", payload);
            System.out.println("📤 [WS] Payload envoyé sur /topic/chat-messages");

        } catch (JsonProcessingException e) {
            System.err.println("❌ [WS] Erreur désérialisation JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ [WS] Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void receiveApproveContract(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            Map<String, Object> payload = mapper.readValue(message, new TypeReference<Map<String, Object>>() {
            });
            messagingTemplate.convertAndSend("/topic/approve-contract", payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


}