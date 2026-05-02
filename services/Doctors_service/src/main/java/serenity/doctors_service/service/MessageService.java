package serenity.doctors_service.service;

import org.springframework.stereotype.Service;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.repository.MessageRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService implements IMessageService {

    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;

    public MessageService(MessageRepository messageRepository, ConversationMapper conversationMapper) {
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public MessageDTO sendMessage(Conversation conversation, Long senderId, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setRead(false);
        Message saved = messageRepository.save(message);
        return conversationMapper.toMessageDTO(saved);
    }

    @Override
    public List<MessageDTO> getMessages(Conversation conversation) {
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        return messages.stream()
                .map(conversationMapper::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MessageDTO> getMessageById(Long id) {
        return messageRepository.findById(id)
                .map(conversationMapper::toMessageDTO);
    }

    @Override
    public MessageDTO editMessage(Long messageId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setContent(newContent);
        Message updated = messageRepository.save(message);
        return conversationMapper.toMessageDTO(updated);
    }

    @Override
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    @Override
    public MessageDTO markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setRead(true);
        Message updated = messageRepository.save(message);
        return conversationMapper.toMessageDTO(updated);
    }
}