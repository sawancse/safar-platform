package com.safar.messaging.controller;

import com.safar.messaging.dto.ConversationResponse;
import com.safar.messaging.dto.MessageResponse;
import com.safar.messaging.dto.SendMessageRequest;
import com.safar.messaging.entity.QuickReplyTemplate;
import com.safar.messaging.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@Valid @RequestBody SendMessageRequest req,
                                       Authentication auth) {
        UUID senderId = UUID.fromString(auth.getName());
        return messagingService.sendMessage(senderId, req);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> getConversations(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return messagingService.getConversations(userId);
    }

    @GetMapping("/conversations/{conversationId}")
    public Page<MessageResponse> getMessages(@PathVariable UUID conversationId,
                                              @PageableDefault(size = 50) Pageable pageable,
                                              Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return messagingService.getMessages(userId, conversationId, pageable);
    }

    @PostMapping("/conversations/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID conversationId,
                           Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        messagingService.markAsRead(userId, conversationId);
    }

    @GetMapping("/unread-count")
    public Map<String, Integer> getUnreadCount(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return Map.of("unreadCount", messagingService.getUnreadCount(userId));
    }

    @GetMapping("/quick-replies")
    public List<QuickReplyTemplate> getQuickReplies(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return messagingService.getQuickReplies(userId);
    }

    @PostMapping("/quick-replies")
    @ResponseStatus(HttpStatus.CREATED)
    public QuickReplyTemplate createQuickReply(@RequestBody Map<String, String> body,
                                                Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        return messagingService.createQuickReply(userId, content);
    }

    @DeleteMapping("/quick-replies/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuickReply(@PathVariable UUID templateId,
                                  Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        messagingService.deleteQuickReply(userId, templateId);
    }
}
