package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "channel_sync_logs", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_manager_property_id", nullable = false)
    private UUID channelManagerPropertyId;

    @Column(nullable = false, length = 10)
    private String direction; // PUSH, PULL

    @Column(name = "sync_type", nullable = false, length = 20)
    private String syncType; // RATE, AVAILABILITY, CONTENT, BOOKING

    @Column(name = "channel_name", length = 30)
    private String channelName;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "records_affected", nullable = false)
    @Builder.Default
    private int recordsAffected = 0;

    @Column(name = "synced_at", nullable = false)
    @Builder.Default
    private OffsetDateTime syncedAt = OffsetDateTime.now();
}
