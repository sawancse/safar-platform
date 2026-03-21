package com.safar.messaging.repository;

import com.safar.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID senderId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :readAt WHERE m.conversationId = :conversationId AND m.senderId <> :userId AND m.readAt IS NULL")
    int markAllAsRead(UUID conversationId, UUID userId, OffsetDateTime readAt);
}
