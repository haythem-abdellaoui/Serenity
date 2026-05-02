package serenity.doctors_service.mapper;

import org.springframework.stereotype.Service;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;

import java.util.stream.Collectors;

@Service
public class ConversationMapper {

    public ConversationDTO toDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setUser1Id(conversation.getUser1Id());
        dto.setUser2Id(conversation.getUser2Id());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        if (conversation.getMessages() != null) {
            dto.setMessages(conversation.getMessages().stream()
                    .map(this::toMessageDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public MessageDTO toMessageDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setSenderId(message.getSenderId());
        dto.setConversationId(message.getConversation().getId());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setIsRead(message.isRead());
        return dto;
    }
}
