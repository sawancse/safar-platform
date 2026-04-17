package com.safar.messaging.controller;

import com.safar.messaging.dto.ConversationResponse;
import com.safar.messaging.dto.MessageResponse;
import com.safar.messaging.dto.SendMessageRequest;
import com.safar.messaging.entity.QuickReplyTemplate;
import com.safar.messaging.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    @Value("${services.media-service.url:http://localhost:8088}")
    private String mediaServiceUrl;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@Valid @RequestBody SendMessageRequest req,
                                       Authentication auth) {
        UUID senderId = UUID.fromString(auth.getName());
        return messagingService.sendMessage(senderId, req);
    }

    /**
     * Upload a file for chat attachment. Returns metadata + S3 URL.
     * Max 10MB. Supported: PDF, DOC, DOCX, JPG, PNG, WebP.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                 Authentication auth) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) throw new IllegalArgumentException("File exceeds 10MB limit");

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        // Validate file type
        java.util.Set<String> allowed = java.util.Set.of(
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/jpeg", "image/png", "image/webp");
        if (!allowed.contains(mime)) {
            throw new IllegalArgumentException("Unsupported file type: " + mime + ". Allowed: PDF, DOC, DOCX, JPG, PNG, WebP");
        }

        // Upload to media-service (S3) via REST
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("folder", "chat");

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mediaServiceUrl + "/api/v1/media/internal/upload", entity, Map.class);

            String url = response != null && response.get("url") != null ? response.get("url").toString() : "";

            return Map.of(
                    "attachmentUrl", url,
                    "fileName", originalName,
                    "fileSize", file.getSize(),
                    "mimeType", mime
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Internal endpoint for service-to-service message sending (chef-service, booking-service).
     * Uses X-User-Id header instead of JWT auth.
     */
    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendInternalMessage(@RequestBody SendMessageRequest req,
                                                @RequestHeader("X-User-Id") UUID senderId) {
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
