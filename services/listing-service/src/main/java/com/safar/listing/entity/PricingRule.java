package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_rules", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "room_type_id")
    private UUID roomTypeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "price_adjustment_type", nullable = false, length = 20)
    private String priceAdjustmentType;

    @Column(name = "adjustment_value", nullable = false)
    private Long adjustmentValue;

    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
