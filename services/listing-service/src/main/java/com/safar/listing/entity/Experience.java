package com.safar.listing.entity;

import com.safar.listing.entity.enums.ExperienceCategory;
import com.safar.listing.entity.enums.ExperienceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "experiences", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExperienceCategory category;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "location_name", length = 200)
    private String locationName;

    @Column(name = "duration_hours", nullable = false)
    private BigDecimal durationHours;

    @Column(name = "max_guests", nullable = false)
    @Builder.Default
    private Integer maxGuests = 10;

    @Column(name = "price_paise", nullable = false)
    private Long pricePaise;

    @Column(name = "languages_spoken", nullable = false)
    @Builder.Default
    private String languagesSpoken = "en";

    @Column(name = "media_urls", nullable = false)
    @Builder.Default
    private String mediaUrls = "";

    @Column(name = "whats_included", columnDefinition = "TEXT")
    private String whatsIncluded;

    @Column(name = "whats_not_included", columnDefinition = "TEXT")
    private String whatsNotIncluded;

    @Column(columnDefinition = "TEXT")
    private String itinerary;

    @Column(name = "meeting_point", columnDefinition = "TEXT")
    private String meetingPoint;

    @Column(name = "meeting_point_lat", precision = 9, scale = 6)
    private BigDecimal meetingPointLat;

    @Column(name = "meeting_point_lng", precision = 9, scale = 6)
    private BigDecimal meetingPointLng;

    @Column(columnDefinition = "TEXT")
    private String accessibility;

    @Column(name = "cancellation_policy", length = 20)
    @Builder.Default
    private String cancellationPolicy = "FLEXIBLE";

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "is_private")
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "group_discount_pct")
    private Integer groupDiscountPct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExperienceStatus status = ExperienceStatus.DRAFT;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
