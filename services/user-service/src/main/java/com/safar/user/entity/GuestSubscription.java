package com.safar.user.entity;

import com.safar.user.entity.enums.GuestSubStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guest_subscriptions", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuestSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID guestId;

    private String razorpaySubId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GuestSubStatus status = GuestSubStatus.ACTIVE;

    private OffsetDateTime trialEndsAt;
    private OffsetDateTime nextBillingAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
