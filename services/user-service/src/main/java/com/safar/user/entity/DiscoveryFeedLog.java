package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "discovery_feed_log", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DiscoveryFeedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID guestId;

    private String listingIds;

    private String algorithm;

    @CreationTimestamp
    private OffsetDateTime generatedAt;
}
