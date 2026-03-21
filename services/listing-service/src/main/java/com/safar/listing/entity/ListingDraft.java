package com.safar.listing.entity;

import com.safar.listing.entity.enums.DraftStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_drafts", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ListingDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String type;

    @Column(name = "ai_title")
    private String aiTitle;

    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;

    /** Comma-separated amenity list */
    @Column(name = "ai_amenities", columnDefinition = "TEXT")
    private String aiAmenities;

    @Column(name = "ai_suggested_price_paise")
    private Long aiSuggestedPricePaise;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private DraftStatus status = DraftStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
