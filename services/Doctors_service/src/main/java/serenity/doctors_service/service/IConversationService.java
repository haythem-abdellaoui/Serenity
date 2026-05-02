package serenity.doctors_service.service;

import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.dto.ConversationDTO2;
import serenity.doctors_service.dto.ConversationKeywordResultDTO;
import serenity.doctors_service.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface IConversationService {
    ConversationDTO createConversation(Long user1Id, Long user2Id);

    List<ConversationDTO> getUserConversations(Long userId);

    Optional<ConversationDTO> getConversationById(Long id);

    void deleteConversation(Long id);

    Conversation createOrGetConversation(Long user1Id, Long user2Id);

    String analyzeConversation(Long conversationId);

    List<ConversationDTO2> getConversations();

    List<ConversationKeywordResultDTO> searchConversationsByKeyword(String keyword);
}
