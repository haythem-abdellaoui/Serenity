package serenity.doctors_service.dto;

import java.util.List;

public class MessagesRequest {
    private List<String> messages;

    public MessagesRequest(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}