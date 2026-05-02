package serenity.doctors_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import serenity.doctors_service.dto.ConversationDTO;
import serenity.doctors_service.dto.ConversationDTO2;
import serenity.doctors_service.dto.ConversationKeywordResultDTO;
import serenity.doctors_service.entity.Conversation;
import serenity.doctors_service.mapper.ConversationMapper;
import serenity.doctors_service.service.IConversationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final IConversationService conversationService;

    private ConversationMapper conversationMapper;

    public ConversationController(IConversationService conversationService,
                                  ConversationMapper conversationMapper) {
        this.conversationService = conversationService;
        this.conversationMapper = conversationMapper;
    }

    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {
        ConversationDTO conversation = conversationService.createConversation(user1Id, user2Id);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(@PathVariable Long userId) {
        List<ConversationDTO> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getConversation(@PathVariable Long id) {
        return conversationService.getConversationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/start")
    public ResponseEntity<ConversationDTO> startConversation(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {

        Conversation conversation = conversationService.createOrGetConversation(user1Id, user2Id);
        return ResponseEntity.ok(conversationMapper.toDTO(conversation));
    }

    @GetMapping("/{id}/analyze_conversation")
    public String analyzeConversation(@PathVariable("id") Long conversationId) {
        return conversationService.analyzeConversation(conversationId);
    }

    @GetMapping("/conversations-summary")
    public ResponseEntity<List<ConversationDTO2>> getConversations() {
        return ResponseEntity.ok(conversationService.getConversations());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ConversationKeywordResultDTO>> searchConversationsByKeyword(
            @RequestParam String keyword) {
        return ResponseEntity.ok(conversationService.searchConversationsByKeyword(keyword));
    }
}