package com.safar.listing.entity;

import com.safar.listing.entity.enums.BuilderListingStatus;
import com.safar.listing.entity.enums.ProjectStatus;
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
@Table(name = "builder_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "builder_id", nullable = false)
    private UUID builderId;

    // ── Builder Info ──
    @Column(name = "builder_name", nullable = false, length = 200)
    private String builderName;

    @Column(name = "builder_logo_url", length = 500)
    private String builderLogoUrl;

    // ── Project Info ──
    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(length = 300)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── RERA ──
    @Column(name = "rera_id", length = 100)
    private String reraId;

    @Builder.Default
    @Column(name = "rera_verified")
    private Boolean reraVerified = false;

    // ── Location ──
    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(length = 100)
    private String locality;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(columnDefinition = "TEXT")
    private String address;

    // ── Scale ──
    @Column(name = "total_units")
    private Integer totalUnits;

    @Column(name = "available_units")
    private Integer availableUnits;

    @Column(name = "total_towers")
    private Integer totalTowers;

    @Column(name = "total_floors_max")
    private Integer totalFloorsMax;

    // ── Construction ──
    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false)
    @Builder.Default
    private ProjectStatus projectStatus = ProjectStatus.UNDER_CONSTRUCTION;

    @Column(name = "launch_date")
    private LocalDate launchDate;

    @Column(name = "possession_date")
    private LocalDate possessionDate;

    @Column(name = "construction_progress_percent")
    @Builder.Default
    private Integer constructionProgressPercent = 0;

    // ── Area ──
    @Column(name = "land_area_sqft")
    private Integer landAreaSqft;

    @Column(name = "project_area_sqft")
    private Integer projectAreaSqft;

    // ── Amenities ──
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> amenities;

    // ── Media ──
    @Column(name = "master_plan_url", length = 500)
    private String masterPlanUrl;

    @Column(name = "brochure_url", length = 500)
    private String brochureUrl;

    @Column(name = "walkthrough_url", length = 500)
    private String walkthroughUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> photos;

    // ── Approvals ──
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "bank_approvals", columnDefinition = "text[]")
    private List<String> bankApprovals;

    // ── Payment Plans (JSONB) ──
    @Column(name = "payment_plans", columnDefinition = "TEXT")
    private String paymentPlansJson;

    // ── Pricing Range (computed from unit types) ──
    @Column(name = "min_price_paise")
    private Long minPricePaise;

    @Column(name = "max_price_paise")
    private Long maxPricePaise;

    @Column(name = "min_bhk")
    private Integer minBhk;

    @Column(name = "max_bhk")
    private Integer maxBhk;

    @Column(name = "min_area_sqft")
    private Integer minAreaSqft;

    @Column(name = "max_area_sqft")
    private Integer maxAreaSqft;

    // ── Status ──
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BuilderListingStatus status = BuilderListingStatus.DRAFT;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    @Column(name = "views_count")
    private Integer viewsCount = 0;

    @Builder.Default
    @Column(name = "inquiries_count")
    private Integer inquiriesCount = 0;

    // ── Audit ──
    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
