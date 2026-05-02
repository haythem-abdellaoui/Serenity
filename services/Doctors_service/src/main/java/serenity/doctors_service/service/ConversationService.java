package serenity.doctors_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.dto.ConversationDTO2;
import serenity.doctors_service.dto.ConversationKeywordResultDTO;
import serenity.doctors_service.dto.MessagesRequest;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.repository.ConversationRepository;
import serenity.doctors_service.repository.MessageRepository;
import serenity.doctors_service.entity.Message;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class ConversationService implements IConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMapper conversationMapper;
    private final RestTemplate restTemplate;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMapper conversationMapper,
                               RestTemplate restTemplate,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationMapper = conversationMapper;
        this.restTemplate = restTemplate;
        this.messageRepository = messageRepository;
    }

    @Override
    public ConversationDTO createConversation(Long user1Id, Long user2Id) {
        Conversation conversation = new Conversation();
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        Conversation saved = conversationRepository.save(conversation);
        return conversationMapper.toDTO(saved);
    }

    @Override
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUser1IdOrUser2Id(userId, userId);
        return conversations.stream()
                .map(conversationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ConversationDTO> getConversationById(Long id) {
        return conversationRepository.findById(id)
                .map(conversationMapper::toDTO);
    }

    @Override
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }

    @Override
    public Conversation createOrGetConversation(Long user1Id, Long user2Id) {
        Optional<Conversation> existing = conversationRepository
                .findByUser1IdAndUser2Id(user1Id, user2Id)
                .or(() -> conversationRepository.findByUser1IdAndUser2Id(user2Id, user1Id));

        if (existing.isPresent()) return existing.get();

        Conversation conversation = new Conversation();
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        return conversationRepository.save(conversation);
    }

    public List<String> getMessagesText(Long conversationId) {
        return messageRepository.findByConversationId(conversationId)
                .stream()
                .map(Message::getContent)
                .toList();
    }

    @Override
    public String analyzeConversation(Long conversationId) {
        List<String> messages = getMessagesText(conversationId);

        String url = "http://localhost:8000/predict-conversation";

        MessagesRequest request = new MessagesRequest(messages);

        return restTemplate.postForObject(url, request, String.class);
    }

    @Override
    public List<ConversationDTO2> getConversations() {

        List<Object[]> results = conversationRepository.findConversationsWithLastMessage();

        return results.stream().map(r -> {
            Conversation c = (Conversation) r[0];
            Message m = (Message) r[1];

            return new ConversationDTO2(
                    c.getId(),
                    m != null ? m.getContent() : null,
                    m != null ? m.getCreatedAt() : null
            );
        }).toList();
    }

    @Override
    public List<ConversationKeywordResultDTO> searchConversationsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        List<Object[]> rows = conversationRepository.searchConversationsByKeyword(keyword.trim());

        return rows.stream().map(row -> {
            Conversation conversation = (Conversation) row[0];
            Message lastMatchedMessage = (Message) row[1];
            Long matchedCount = row[2] instanceof Number n ? n.longValue() : 0L;

            return new ConversationKeywordResultDTO(
                    conversation.getId(),
                    conversation.getUser1Id(),
                    conversation.getUser2Id(),
                    matchedCount,
                    lastMatchedMessage.getContent(),
                    lastMatchedMessage.getCreatedAt()
            );
        }).toList();
    }
}
