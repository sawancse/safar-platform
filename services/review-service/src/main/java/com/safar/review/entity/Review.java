package com.safar.review.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews", schema = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", unique = true, nullable = false)
    private UUID bookingId;

    @Column(name = "target_type", length = 20)
    @Builder.Default
    private String targetType = "LISTING";

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "experience_id")
    private UUID experienceId;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Min(1) @Max(5)
    @Column(nullable = false, columnDefinition = "SMALLINT")
    private Short rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "replied_at")
    private OffsetDateTime repliedAt;

    @Column(name = "reply_updated_at")
    private OffsetDateTime replyUpdatedAt;

    @Column(name = "guest_photo_urls", columnDefinition = "TEXT")
    private String guestPhotoUrls;

    @Column(name = "guest_name", length = 200)
    private String guestName;

    @Min(1) @Max(5)
    @Column(name = "rating_cleanliness", columnDefinition = "SMALLINT")
    private Short ratingCleanliness;

    @Min(1) @Max(5)
    @Column(name = "rating_location", columnDefinition = "SMALLINT")
    private Short ratingLocation;

    @Min(1) @Max(5)
    @Column(name = "rating_value", columnDefinition = "SMALLINT")
    private Short ratingValue;

    @Min(1) @Max(5)
    @Column(name = "rating_communication", columnDefinition = "SMALLINT")
    private Short ratingCommunication;

    @Min(1) @Max(5)
    @Column(name = "rating_check_in", columnDefinition = "SMALLINT")
    private Short ratingCheckIn;

    @Min(1) @Max(5)
    @Column(name = "rating_accuracy", columnDefinition = "SMALLINT")
    private Short ratingAccuracy;

    @Min(1) @Max(5)
    @Column(name = "rating_staff", columnDefinition = "SMALLINT")
    private Short ratingStaff;

    @Min(1) @Max(5)
    @Column(name = "rating_facilities", columnDefinition = "SMALLINT")
    private Short ratingFacilities;

    @Min(1) @Max(5)
    @Column(name = "rating_comfort", columnDefinition = "SMALLINT")
    private Short ratingComfort;

    @Min(1) @Max(5)
    @Column(name = "rating_free_wifi", columnDefinition = "SMALLINT")
    private Short ratingFreeWifi;

    // Per-category text comments as JSON: {"cleanliness":"...", "location":"..."}
    @Column(name = "category_comments", columnDefinition = "TEXT")
    private String categoryComments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Double-blind review system (Airbnb model) ──
    // Host's review of guest (submitted independently)
    @Min(1) @Max(5)
    @Column(name = "host_rating", columnDefinition = "SMALLINT")
    private Short hostRating;

    @Column(name = "host_comment", columnDefinition = "TEXT")
    private String hostComment;

    @Column(name = "host_reviewed_at")
    private OffsetDateTime hostReviewedAt;

    // Visibility: both reviews hidden until BOTH submitted OR deadline expires
    @Column(name = "guest_review_visible")
    @Builder.Default
    private Boolean guestReviewVisible = false;

    @Column(name = "host_review_visible")
    @Builder.Default
    private Boolean hostReviewVisible = false;

    @Column(name = "both_revealed_at")
    private OffsetDateTime bothRevealedAt;

    // 14-day review window after checkout
    @Column(name = "review_deadline")
    private OffsetDateTime reviewDeadline;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
