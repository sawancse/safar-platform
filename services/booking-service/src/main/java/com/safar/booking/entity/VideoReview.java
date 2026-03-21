package com.safar.booking.entity;

import com.safar.booking.entity.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_reviews", schema = "bookings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VideoReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private UUID listingId;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    private String cdnUrl;

    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    private String moderationReason;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
