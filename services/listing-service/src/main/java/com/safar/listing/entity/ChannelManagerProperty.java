package com.safar.listing.entity;

import com.safar.listing.entity.enums.ChannelStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "channel_manager_properties", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelManagerProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "channex_property_id", length = 100)
    private String channexPropertyId;

    @Column(name = "channex_group_id", length = 100)
    private String channexGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChannelStatus status = ChannelStatus.PENDING;

    @Column(name = "connected_channels", columnDefinition = "TEXT")
    private String connectedChannels;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
