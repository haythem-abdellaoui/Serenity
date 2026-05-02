package serenity.doctors_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    List<Message> findByConversationId(Long conversationId);
}
