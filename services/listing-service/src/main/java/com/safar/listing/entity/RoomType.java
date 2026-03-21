package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.safar.listing.entity.enums.SharingType;
import com.safar.listing.entity.enums.StayMode;
import com.safar.listing.entity.enums.RoomVariant;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_types", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer count = 1;

    @Column(name = "base_price_paise", nullable = false)
    private Long basePricePaise;

    @Column(name = "max_guests", nullable = false)
    @Builder.Default
    private Integer maxGuests = 2;

    @Column(name = "bed_type", length = 50)
    private String bedType;

    @Column(name = "bed_count")
    @Builder.Default
    private Integer bedCount = 1;

    @Column(name = "area_sqft")
    private Integer areaSqft;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Enumerated(EnumType.STRING)
    @Column(name = "stay_mode")
    @Builder.Default
    private StayMode stayMode = StayMode.NIGHTLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_type")
    private SharingType sharingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_variant")
    private RoomVariant roomVariant;

    @Column(name = "total_beds")
    private Integer totalBeds;

    @Column(name = "occupied_beds")
    @Builder.Default
    private Integer occupiedBeds = 0;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // Room photos (Booking.com style — primary + up to 4 more)
    @Column(name = "primary_photo_url", length = 500)
    private String primaryPhotoUrl;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls; // comma-separated URLs

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
