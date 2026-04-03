package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sale_properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SellerType sellerType = SellerType.OWNER;

    // Link to existing rental listing (optional, for dual-purpose)
    private UUID linkedListingId;

    // ── BASIC INFO ──
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalePropertyType salePropertyType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionType transactionType = TransactionType.RESALE;

    // ── LOCATION ──
    @Column(length = 500)
    private String addressLine1;

    @Column(length = 500)
    private String addressLine2;

    @Column(length = 100)
    private String locality;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(length = 200)
    private String landmark;

    // ── PRICING ──
    @Column(nullable = false)
    private Long askingPricePaise;

    private Long pricePerSqftPaise;

    @Builder.Default
    private Boolean priceNegotiable = false;

    private Long maintenancePaise;

    private Long bookingAmountPaise;

    // ── AREA & DIMENSIONS ──
    private Integer carpetAreaSqft;

    private Integer builtUpAreaSqft;

    private Integer superBuiltUpAreaSqft;

    private Integer plotAreaSqft;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AreaUnit areaUnit = AreaUnit.SQFT;

    // ── CONFIGURATION ──
    private Integer bedrooms;

    private Integer bathrooms;

    private Integer balconies;

    private Integer floorNumber;

    private Integer totalFloors;

    @Enumerated(EnumType.STRING)
    private FacingDirection facing;

    private Integer propertyAgeYears;

    @Enumerated(EnumType.STRING)
    private FurnishingStatus furnishing;

    @Builder.Default
    private Integer parkingCovered = 0;

    @Builder.Default
    private Integer parkingOpen = 0;

    // ── CONSTRUCTION & LEGAL ──
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PossessionStatus possessionStatus = PossessionStatus.READY_TO_MOVE;

    private LocalDate possessionDate;

    @Column(length = 200)
    private String builderName;

    @Column(length = 200)
    private String projectName;

    @Column(length = 100)
    private String reraId;

    @Builder.Default
    private Boolean reraVerified = false;

    // ── FEATURES ──
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> amenities;

    @Enumerated(EnumType.STRING)
    private WaterSupply waterSupply;

    @Enumerated(EnumType.STRING)
    private PowerBackup powerBackup;

    @Builder.Default
    private Boolean gatedCommunity = false;

    @Builder.Default
    private Boolean cornerProperty = false;

    @Builder.Default
    private Boolean vastuCompliant = false;

    @Builder.Default
    private Boolean petAllowed = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> overlooking;

    // ── MEDIA ──
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> photos;

    private String floorPlanUrl;

    private String videoTourUrl;

    private String brochureUrl;

    // ── STATUS ──
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SalePropertyStatus status = SalePropertyStatus.DRAFT;

    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Integer viewsCount = 0;

    @Builder.Default
    private Integer inquiriesCount = 0;

    private OffsetDateTime expiresAt;

    // ── AUDIT ──
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private OffsetDateTime approvedAt;

    @PrePersist
    public void prePersist() {
        if (expiresAt == null) {
            expiresAt = OffsetDateTime.now().plusDays(90);
        }
        computePricePerSqft();
    }

    @PreUpdate
    public void preUpdate() {
        computePricePerSqft();
    }

    private void computePricePerSqft() {
        int area = carpetAreaSqft != null ? carpetAreaSqft
                : builtUpAreaSqft != null ? builtUpAreaSqft
                : superBuiltUpAreaSqft != null ? superBuiltUpAreaSqft : 0;
        if (area > 0 && askingPricePaise != null) {
            this.pricePerSqftPaise = askingPricePaise / area;
        }
    }
}
