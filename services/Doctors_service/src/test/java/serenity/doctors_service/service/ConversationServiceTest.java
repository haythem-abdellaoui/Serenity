package serenity.doctors_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.dto.ConversationDTO2;
import serenity.doctors_service.dto.ConversationKeywordResultDTO;
import serenity.doctors_service.dto.MessagesRequest;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.entity.Message;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.repository.ConversationRepository;
import serenity.doctors_service.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void shouldCreateConversation() {
        Conversation saved = new Conversation();
        saved.setId(10L);
        saved.setUser1Id(1L);
        saved.setUser2Id(2L);

        ConversationDTO dto = new ConversationDTO();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(conversationMapper.toDTO(saved)).thenReturn(dto);

        ConversationDTO result = conversationService.createConversation(1L, 2L);

        assertSame(dto, result);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUser1Id());
        assertEquals(2L, captor.getValue().getUser2Id());

        verify(conversationMapper).toDTO(saved);
    }

    @Test
    void shouldReturnUserConversations() {
        Conversation c1 = new Conversation();
        c1.setId(1L);
        Conversation c2 = new Conversation();
        c2.setId(2L);

        ConversationDTO dto1 = new ConversationDTO();
        dto1.setId(1L);
        ConversationDTO dto2 = new ConversationDTO();
        dto2.setId(2L);

        when(conversationRepository.findByUser1IdOrUser2Id(1L, 1L))
                .thenReturn(List.of(c1, c2));
        when(conversationMapper.toDTO(c1)).thenReturn(dto1);
        when(conversationMapper.toDTO(c2)).thenReturn(dto2);

        List<ConversationDTO> result = conversationService.getUserConversations(1L);

        assertEquals(2, result.size());
        assertSame(dto1, result.get(0));
        assertSame(dto2, result.get(1));

        verify(conversationRepository).findByUser1IdOrUser2Id(1L, 1L);
    }

    @Test
    void shouldGetConversationByIdWhenPresent() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);

        ConversationDTO dto = new ConversationDTO();
        dto.setId(1L);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(conversationMapper.toDTO(conversation)).thenReturn(dto);

        Optional<ConversationDTO> result = conversationService.getConversationById(1L);

        assertTrue(result.isPresent());
        assertSame(dto, result.get());
    }

    @Test
    void shouldReturnEmptyConversationByIdWhenNotFound() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<ConversationDTO> result = conversationService.getConversationById(1L);

        assertTrue(result.isEmpty());
        verify(conversationMapper, never()).toDTO(any());
    }

    @Test
    void shouldDeleteConversation() {
        conversationService.deleteConversation(1L);

        verify(conversationRepository).deleteById(1L);
    }

    @Test
    void shouldReturnExistingConversationInCreateOrGetConversation() {
        Conversation existing = new Conversation();
        existing.setId(99L);
        existing.setUser1Id(1L);
        existing.setUser2Id(2L);

        when(conversationRepository.findByUser1IdAndUser2Id(1L, 2L))
                .thenReturn(Optional.of(existing));

        Conversation result = conversationService.createOrGetConversation(1L, 2L);

        assertSame(existing, result);
        verify(conversationRepository).findByUser1IdAndUser2Id(1L, 2L);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void shouldCreateConversationWhenNoneExistsInCreateOrGetConversation() {
        when(conversationRepository.findByUser1IdAndUser2Id(1L, 2L))
                .thenReturn(Optional.empty());
        when(conversationRepository.findByUser1IdAndUser2Id(2L, 1L))
                .thenReturn(Optional.empty());

        Conversation saved = new Conversation();
        saved.setId(50L);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

        Conversation result = conversationService.createOrGetConversation(1L, 2L);

        assertSame(saved, result);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUser1Id());
        assertEquals(2L, captor.getValue().getUser2Id());
    }

    @Test
    void shouldReturnMessagesText() {
        Message m1 = new Message();
        m1.setContent("hello");
        Message m2 = new Message();
        m2.setContent("world");

        when(messageRepository.findByConversationId(1L)).thenReturn(List.of(m1, m2));

        List<String> result = conversationService.getMessagesText(1L);

        assertEquals(List.of("hello", "world"), result);
        verify(messageRepository).findByConversationId(1L);
    }

    @Test
    void shouldAnalyzeConversation() {
        Message m1 = new Message();
        m1.setContent("first");
        Message m2 = new Message();
        m2.setContent("second");

        when(messageRepository.findByConversationId(1L)).thenReturn(List.of(m1, m2));
        when(restTemplate.postForObject(
                eq("http://localhost:8000/predict-conversation"),
                any(MessagesRequest.class),
                eq(String.class)
        )).thenReturn("positive");

        String result = conversationService.analyzeConversation(1L);

        assertEquals("positive", result);

        ArgumentCaptor<MessagesRequest> captor = ArgumentCaptor.forClass(MessagesRequest.class);
        verify(restTemplate).postForObject(
                eq("http://localhost:8000/predict-conversation"),
                captor.capture(),
                eq(String.class)
        );
        assertEquals(List.of("first", "second"), captor.getValue().getMessages());
    }

    @Test
    void shouldReturnConversationsSummaryWithLastMessage() {
        Conversation c1 = new Conversation();
        c1.setId(1L);

        Message m1 = new Message();
        m1.setContent("latest message");
        m1.setCreatedAt(LocalDateTime.of(2026, 4, 9, 12, 0));

        Conversation c2 = new Conversation();
        c2.setId(2L);

        List<Object[]> rows = List.of(
                new Object[]{c1, m1},
                new Object[]{c2, null}
        );

        when(conversationRepository.findConversationsWithLastMessage()).thenReturn(rows);

        List<ConversationDTO2> result = conversationService.getConversations();

        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).getConversationId());
        assertEquals("latest message", result.get(0).getLastMessage());
        assertEquals(LocalDateTime.of(2026, 4, 9, 12, 0), result.get(0).getLastMessageTime());

        assertEquals(2L, result.get(1).getConversationId());
        assertNull(result.get(1).getLastMessage());
        assertNull(result.get(1).getLastMessageTime());
    }

    @Test
    void shouldReturnEmptyListWhenKeywordIsNull() {
        List<ConversationKeywordResultDTO> result = conversationService.searchConversationsByKeyword(null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void shouldReturnEmptyListWhenKeywordIsBlank() {
        List<ConversationKeywordResultDTO> result = conversationService.searchConversationsByKeyword("   ");

        assertTrue(result.isEmpty());
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void shouldSearchConversationsByKeyword() {
        Conversation conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUser1Id(100L);
        conversation.setUser2Id(200L);

        Message matched = new Message();
        matched.setContent("keyword matched here");
        matched.setCreatedAt(LocalDateTime.of(2026, 4, 9, 13, 30));

        List<Object[]> rows = List.<Object[]>of(
                new Object[]{conversation, matched, Integer.valueOf(3)}
        );

        when(conversationRepository.searchConversationsByKeyword("keyword")).thenReturn(rows);

        List<ConversationKeywordResultDTO> result = conversationService.searchConversationsByKeyword("keyword");

        assertEquals(1, result.size());

        ConversationKeywordResultDTO dto = result.get(0);
        assertEquals(10L, dto.getConversationId());
        assertEquals(100L, dto.getUser1Id());
        assertEquals(200L, dto.getUser2Id());
        assertEquals(3L, dto.getMatchedMessagesCount());
        assertEquals("keyword matched here", dto.getLastMatchedMessage());
        assertEquals(LocalDateTime.of(2026, 4, 9, 13, 30), dto.getLastMatchedAt());

        verify(conversationRepository).searchConversationsByKeyword("keyword");
    }
}