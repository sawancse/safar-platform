package com.safar.messaging.service;

import com.safar.messaging.dto.ConversationResponse;
import com.safar.messaging.dto.MessageResponse;
import com.safar.messaging.dto.SendMessageRequest;
import com.safar.messaging.entity.Conversation;
import com.safar.messaging.entity.Message;
import com.safar.messaging.entity.QuickReplyTemplate;
import com.safar.messaging.repository.ConversationRepository;
import com.safar.messaging.repository.MessageRepository;
import com.safar.messaging.repository.QuickReplyTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final QuickReplyTemplateRepository quickReplyTemplateRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest req) {
        UUID recipientId = req.recipientId();
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }

        // Ensure deterministic participant ordering: participant1 = min, participant2 = max
        UUID participant1 = senderId.compareTo(recipientId) < 0 ? senderId : recipientId;
        UUID participant2 = senderId.compareTo(recipientId) < 0 ? recipientId : senderId;

        Conversation conversation = conversationRepository
                .findByParticipant1IdAndParticipant2IdAndListingId(participant1, participant2, req.listingId())
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .participant1Id(participant1)
                            .participant2Id(participant2)
                            .listingId(req.listingId())
                            .bookingId(req.bookingId())
                            .build();
                    return conversationRepository.save(newConv);
                });

        // Link booking if provided and not already linked
        if (req.bookingId() != null && conversation.getBookingId() == null) {
            conversation.setBookingId(req.bookingId());
        }

        // Determine message type
        String msgType = req.messageType() != null ? req.messageType() : "TEXT";
        String contentText = req.content() != null ? req.content() : "";

        // Validate attachment/location based on type
        if (("FILE".equals(msgType) || "IMAGE".equals(msgType)) && (req.attachmentUrl() == null || req.attachmentUrl().isBlank())) {
            throw new IllegalArgumentException("attachmentUrl is required for FILE/IMAGE messages");
        }
        if ("LOCATION".equals(msgType) && (req.latitude() == null || req.longitude() == null)) {
            throw new IllegalArgumentException("latitude and longitude are required for LOCATION messages");
        }

        // Create the message
        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .content(contentText)
                .messageType(msgType)
                .attachmentUrl(req.attachmentUrl())
                .attachmentName(req.attachmentName())
                .attachmentSize(req.attachmentSize())
                .attachmentType(req.attachmentType())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .locationLabel(req.locationLabel())
                .build();
        message = messageRepository.save(message);

        // Update conversation metadata
        String preview = switch (msgType) {
            case "FILE" -> "Shared a file: " + (req.attachmentName() != null ? req.attachmentName() : "document");
            case "IMAGE" -> "Shared an image";
            case "LOCATION" -> req.locationLabel() != null ? req.locationLabel() : "Shared a location";
            default -> contentText;
        };
        conversation.setLastMessageText(truncate(preview, 200));
        conversation.setLastMessageAt(message.getCreatedAt());

        // Increment unread count for the recipient
        if (recipientId.equals(conversation.getParticipant1Id())) {
            conversation.setParticipant1Unread(conversation.getParticipant1Unread() + 1);
        } else {
            conversation.setParticipant2Unread(conversation.getParticipant2Unread() + 1);
        }
        conversationRepository.save(conversation);

        log.info("Message sent: conversationId={}, senderId={}, recipientId={}",
                conversation.getId(), senderId, recipientId);

        MessageResponse response = toMessageResponse(message);

        // Publish Kafka event AFTER transaction commits (don't hold DB connection during Kafka send)
        final UUID convId = conversation.getId();
        final String content = req.content();
        final UUID listing = req.listingId();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishMessageCreatedEvent(convId, senderId, recipientId, listing, content);
                    }
                });

        return response;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository
                .findByParticipant1IdOrParticipant2IdOrderByLastMessageAtDesc(userId, userId);

        return conversations.stream()
                .map(conv -> toConversationResponse(conv, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!userId.equals(conversation.getParticipant1Id()) && !userId.equals(conversation.getParticipant2Id())) {
            throw new SecurityException("You are not a participant of this conversation");
        }

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!userId.equals(conversation.getParticipant1Id()) && !userId.equals(conversation.getParticipant2Id())) {
            throw new SecurityException("You are not a participant of this conversation");
        }

        messageRepository.markAllAsRead(conversationId, userId, OffsetDateTime.now());

        // Reset unread count for this user
        if (userId.equals(conversation.getParticipant1Id())) {
            conversation.setParticipant1Unread(0);
        } else {
            conversation.setParticipant2Unread(0);
        }
        conversationRepository.save(conversation);

        log.info("Marked conversation {} as read for user {}", conversationId, userId);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(UUID userId) {
        List<Conversation> conversations = conversationRepository
                .findByParticipant1IdOrParticipant2IdOrderByLastMessageAtDesc(userId, userId);

        return conversations.stream()
                .mapToInt(conv -> userId.equals(conv.getParticipant1Id())
                        ? conv.getParticipant1Unread()
                        : conv.getParticipant2Unread())
                .sum();
    }

    @Transactional(readOnly = true)
    public List<QuickReplyTemplate> getQuickReplies(UUID userId) {
        return quickReplyTemplateRepository.findByUserIdOrderBySortOrder(userId);
    }

    @Transactional
    public QuickReplyTemplate createQuickReply(UUID userId, String content) {
        QuickReplyTemplate template = QuickReplyTemplate.builder()
                .userId(userId)
                .content(content)
                .build();
        return quickReplyTemplateRepository.save(template);
    }

    @Transactional
    public void deleteQuickReply(UUID userId, UUID templateId) {
        QuickReplyTemplate template = quickReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Quick reply template not found"));

        if (!userId.equals(template.getUserId())) {
            throw new SecurityException("You do not own this quick reply template");
        }

        quickReplyTemplateRepository.delete(template);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void publishMessageCreatedEvent(UUID conversationId, UUID senderId, UUID recipientId,
                                            UUID listingId, String content) {
        try {
            String preview = truncate(content, 100);
            String event = String.format(
                    "{\"conversationId\":\"%s\",\"senderId\":\"%s\",\"recipientId\":\"%s\",\"listingId\":\"%s\",\"contentPreview\":\"%s\"}",
                    conversationId, senderId, recipientId, listingId, preview.replace("\"", "\\\""));
            kafkaTemplate.send("message.created", conversationId.toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish message.created event: {}", e.getMessage());
        }
    }

    private MessageResponse toMessageResponse(Message msg) {
        return new MessageResponse(
                msg.getId(),
                msg.getConversationId(),
                msg.getSenderId(),
                msg.getContent(),
                msg.getMessageType(),
                msg.getAttachmentUrl(),
                msg.getAttachmentName(),
                msg.getAttachmentSize(),
                msg.getAttachmentType(),
                msg.getLatitude(),
                msg.getLongitude(),
                msg.getLocationLabel(),
                msg.getReadAt(),
                msg.getCreatedAt()
        );
    }

    private ConversationResponse toConversationResponse(Conversation conv, UUID userId) {
        int unread = userId.equals(conv.getParticipant1Id())
                ? conv.getParticipant1Unread()
                : conv.getParticipant2Unread();

        return new ConversationResponse(
                conv.getId(),
                conv.getParticipant1Id(),
                conv.getParticipant2Id(),
                conv.getListingId(),
                conv.getBookingId(),
                conv.getLastMessageText(),
                conv.getLastMessageAt(),
                unread,
                conv.getCreatedAt()
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
