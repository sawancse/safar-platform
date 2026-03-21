package com.safar.user.entity;

import com.safar.user.entity.enums.ShareStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_shares", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SocialShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private UUID bookingId;

    private String platform;

    private String shareProofUrl;

    @Builder.Default
    private Long creditsPaise = 29900L;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ShareStatus status = ShareStatus.PENDING;

    private OffsetDateTime verifiedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
