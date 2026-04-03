package com.safar.listing.entity;

import com.safar.listing.entity.enums.FurnishingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_unit_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectUnitType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 100)
    private String name; // "2 BHK Type A", "3 BHK Premium"

    @Column(nullable = false)
    private Integer bhk;

    @Column(name = "carpet_area_sqft")
    private Integer carpetAreaSqft;

    @Column(name = "built_up_area_sqft")
    private Integer builtUpAreaSqft;

    @Column(name = "super_built_up_area_sqft")
    private Integer superBuiltUpAreaSqft;

    // ── Pricing ──
    @Column(name = "base_price_paise", nullable = false)
    private Long basePricePaise;

    @Column(name = "floor_rise_paise")
    @Builder.Default
    private Long floorRisePaise = 0L; // per floor increment

    @Column(name = "facing_premium_paise")
    @Builder.Default
    private Long facingPremiumPaise = 0L; // east/north premium

    @Column(name = "premium_floors_from")
    private Integer premiumFloorsFrom; // floor rise applies from this floor

    // ── Inventory ──
    @Column(name = "total_units")
    private Integer totalUnits;

    @Column(name = "available_units")
    private Integer availableUnits;

    // ── Config ──
    private Integer bathrooms;

    private Integer balconies;

    @Enumerated(EnumType.STRING)
    private FurnishingStatus furnishing;

    // ── Media ──
    @Column(name = "floor_plan_url", length = 500)
    private String floorPlanUrl;

    @Column(name = "unit_layout_url", length = 500)
    private String unitLayoutUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> photos;

    // ── Audit ──
    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
