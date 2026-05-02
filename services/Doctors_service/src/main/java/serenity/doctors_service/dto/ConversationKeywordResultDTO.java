package serenity.doctors_service.dto;

import java.time.LocalDateTime;

public class ConversationKeywordResultDTO {

    private Long conversationId;
    private Long user1Id;
    private Long user2Id;
    private Long matchedMessagesCount;
    private String lastMatchedMessage;
    private LocalDateTime lastMatchedAt;

    public ConversationKeywordResultDTO() {
    }

    public ConversationKeywordResultDTO(Long conversationId,
                                        Long user1Id,
                                        Long user2Id,
                                        Long matchedMessagesCount,
                                        String lastMatchedMessage,
                                        LocalDateTime lastMatchedAt) {
        this.conversationId = conversationId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.matchedMessagesCount = matchedMessagesCount;
        this.lastMatchedMessage = lastMatchedMessage;
        this.lastMatchedAt = lastMatchedAt;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }

    public Long getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
    }

    public Long getMatchedMessagesCount() {
        return matchedMessagesCount;
    }

    public void setMatchedMessagesCount(Long matchedMessagesCount) {
        this.matchedMessagesCount = matchedMessagesCount;
    }

    public String getLastMatchedMessage() {
        return lastMatchedMessage;
    }

    public void setLastMatchedMessage(String lastMatchedMessage) {
        this.lastMatchedMessage = lastMatchedMessage;
    }

    public LocalDateTime getLastMatchedAt() {
        return lastMatchedAt;
    }

    public void setLastMatchedAt(LocalDateTime lastMatchedAt) {
        this.lastMatchedAt = lastMatchedAt;
    }
}

