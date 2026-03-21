package com.safar.messaging.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", schema = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "participant1_id", nullable = false)
    private UUID participant1Id;

    @Column(name = "participant2_id", nullable = false)
    private UUID participant2Id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "last_message_text", columnDefinition = "TEXT")
    private String lastMessageText;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Column(name = "participant1_unread")
    @Builder.Default
    private Integer participant1Unread = 0;

    @Column(name = "participant2_unread")
    @Builder.Default
    private Integer participant2Unread = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
