package com.safar.chef.entity;

import com.safar.chef.entity.enums.ServiceListingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_listings", schema = "services")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "service_type", discriminatorType = DiscriminatorType.STRING, length = 40)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ServiceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_user_id", nullable = false)
    private UUID vendorUserId;

    @Column(name = "service_type", insertable = false, updatable = false, length = 40)
    private String serviceType;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "vendor_slug", nullable = false, length = 100, unique = true)
    private String vendorSlug;

    @Column(name = "hero_image_url", columnDefinition = "TEXT")
    private String heroImageUrl;

    @Column(length = 280)
    private String tagline;

    @Column(name = "about_md", columnDefinition = "TEXT")
    private String aboutMd;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceListingStatus status;

    @Column(name = "status_changed_at")
    private OffsetDateTime statusChangedAt;

    @Column(name = "status_changed_by")
    private UUID statusChangedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> cities;

    @Column(name = "home_city", length = 100)
    private String homeCity;

    @Column(name = "home_pincode", length = 10)
    private String homePincode;

    @Column(name = "home_address", columnDefinition = "TEXT")
    private String homeAddress;

    @Column(name = "home_lat", precision = 9, scale = 6)
    private BigDecimal homeLat;

    @Column(name = "home_lng", precision = 9, scale = 6)
    private BigDecimal homeLng;

    @Column(name = "delivery_radius_km")
    private Integer deliveryRadiusKm;

    @Column(name = "outstation_capable", nullable = false)
    private Boolean outstationCapable = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "delivery_channels", columnDefinition = "varchar(20)[]")
    private List<String> deliveryChannels;

    @Column(name = "pricing_pattern", nullable = false, length = 30)
    private String pricingPattern;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pricing_formula", columnDefinition = "jsonb")
    private String pricingFormula;

    @Column(name = "calendar_mode", length = 20)
    private String calendarMode;

    @Column(name = "default_lead_time_hours")
    private Integer defaultLeadTimeHours;

    @Column(name = "cancellation_policy", length = 30)
    private String cancellationPolicy;

    @Column(name = "cancellation_terms_md", columnDefinition = "TEXT")
    private String cancellationTermsMd;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "completed_bookings_count", nullable = false)
    private Integer completedBookingsCount = 0;

    @Column(name = "trust_tier", nullable = false, length = 20)
    private String trustTier = "LISTED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
