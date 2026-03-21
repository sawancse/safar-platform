package com.safar.listing.entity;

import com.safar.listing.entity.enums.ChannelName;
import com.safar.listing.entity.enums.MappingStatus;
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
@Table(name = "channel_mappings", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_manager_property_id", nullable = false)
    private UUID channelManagerPropertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_name", nullable = false, length = 30)
    private ChannelName channelName;

    @Column(name = "channel_property_id", length = 100)
    private String channelPropertyId;

    @Column(name = "channel_room_type_id", length = 100)
    private String channelRoomTypeId;

    @Column(name = "local_room_type_id")
    private UUID localRoomTypeId;

    @Column(name = "rate_sync", nullable = false)
    @Builder.Default
    private boolean rateSync = true;

    @Column(name = "availability_sync", nullable = false)
    @Builder.Default
    private boolean availabilitySync = true;

    @Column(name = "content_sync", nullable = false)
    @Builder.Default
    private boolean contentSync = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MappingStatus status = MappingStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
