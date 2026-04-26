package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Expo push token registered by the Safar mobile app on first launch.
 * One row per (user, device) so users can have multiple registered
 * devices and we can revoke one without affecting others.
 */
@Entity
@Table(name = "user_push_tokens", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** ExponentPushToken[xxx] format. */
    @Column(nullable = false, length = 200)
    private String pushToken;

    @Column(nullable = false, length = 10)
    private String platform;          // ios / android / web

    @Column(length = 100)
    private String deviceId;

    private Instant lastUsedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
