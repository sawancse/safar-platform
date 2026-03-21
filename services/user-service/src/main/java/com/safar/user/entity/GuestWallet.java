package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guest_wallets", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuestWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID guestId;

    @Builder.Default
    private Long balancePaise = 0L;

    @Builder.Default
    private Long lifetimeEarnedPaise = 0L;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
