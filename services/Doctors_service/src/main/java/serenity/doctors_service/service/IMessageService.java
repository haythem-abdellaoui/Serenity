package serenity.doctors_service.service;

import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;

import java.util.List;
import java.util.Optional;

public interface IMessageService {
    MessageDTO sendMessage(Conversation conversation, Long senderId, String content);

    List<MessageDTO> getMessages(Conversation conversation);

    Optional<MessageDTO> getMessageById(Long id);

    MessageDTO editMessage(Long messageId, String newContent);

    void deleteMessage(Long messageId);

    MessageDTO markAsRead(Long messageId);
}
