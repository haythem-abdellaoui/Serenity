package serenity.doctors_service.dto;

import java.time.LocalDateTime;

public class ConversationDTO2 {

    private Long conversationId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    public ConversationDTO2(Long conversationId, String lastMessage, LocalDateTime lastMessageTime) {
        this.conversationId = conversationId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
