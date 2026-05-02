package serenity.doctors_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import serenity.doctors_service.dto.MessageDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.repository.MessageRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessage_shouldSaveAndReturnDTO() {

        Conversation conversation = new Conversation();

        Message message = new Message();
        message.setContent("hi");

        Message saved = new Message();
        saved.setContent("hi");

        MessageDTO dto = new MessageDTO();

        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(conversationMapper.toMessageDTO(saved)).thenReturn(dto);

        MessageDTO result = messageService.sendMessage(conversation, 1L, "hi");

        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void getMessages_shouldReturnList() {

        Conversation conversation = new Conversation();

        Message msg = new Message();
        msg.setContent("test");

        MessageDTO dto = new MessageDTO();

        when(messageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(List.of(msg));

        when(conversationMapper.toMessageDTO(msg)).thenReturn(dto);

        List<MessageDTO> result = messageService.getMessages(conversation);

        assertEquals(1, result.size());
    }

    @Test
    void getMessageById_shouldReturnOptional() {

        Message msg = new Message();
        MessageDTO dto = new MessageDTO();

        when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));
        when(conversationMapper.toMessageDTO(msg)).thenReturn(dto);

        Optional<MessageDTO> result = messageService.getMessageById(1L);

        assertTrue(result.isPresent());
    }

    @Test
    void editMessage_shouldUpdateContent() {

        Message msg = new Message();
        msg.setContent("old");

        Message updated = new Message();
        updated.setContent("new");

        MessageDTO dto = new MessageDTO();

        when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(Message.class))).thenReturn(updated);
        when(conversationMapper.toMessageDTO(updated)).thenReturn(dto);

        MessageDTO result = messageService.editMessage(1L, "new");

        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void editMessage_shouldThrowException_whenNotFound() {

        when(messageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> messageService.editMessage(1L, "new"));
    }

    @Test
    void deleteMessage_shouldCallRepository() {

        messageService.deleteMessage(1L);

        verify(messageRepository).deleteById(1L);
    }

    @Test
    void markAsRead_shouldUpdateMessage() {

        Message msg = new Message();
        msg.setRead(false);

        Message updated = new Message();
        updated.setRead(true);

        MessageDTO dto = new MessageDTO();

        when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(Message.class))).thenReturn(updated);
        when(conversationMapper.toMessageDTO(updated)).thenReturn(dto);

        MessageDTO result = messageService.markAsRead(1L);

        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void markAsRead_shouldThrowException_whenNotFound() {

        when(messageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> messageService.markAsRead(1L));
    }
}