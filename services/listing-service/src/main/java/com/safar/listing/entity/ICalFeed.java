package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ical_feeds", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ICalFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "feed_url", nullable = false, columnDefinition = "TEXT")
    private String feedUrl;

    @Column(name = "feed_name", length = 100)
    private String feedName;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "sync_interval_hours")
    @Builder.Default
    private Integer syncIntervalHours = 6;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "source_platform", length = 50)
    private String sourcePlatform; // AIRBNB, BOOKING, GOOGLE, CUSTOM

    @Column(name = "last_sync_status", length = 20)
    private String lastSyncStatus; // SUCCESS, FAILED, PARTIAL

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "sync_failure_count")
    @Builder.Default
    private Integer syncFailureCount = 0;

    @Column(name = "etag")
    private String etag;

    @Column(name = "last_modified_header")
    private String lastModifiedHeader;

    @Column(name = "room_type_id")
    private UUID roomTypeId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
