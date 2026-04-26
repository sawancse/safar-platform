package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Platform-level commission rate config — admin-editable without redeploy.
 * One row per (service_type, tier) combo. Seeded by V25 with sensible defaults;
 * admin can edit rates and promotion thresholds via a future admin UI or directly
 * in psql for now.
 *
 * Per-vendor exceptions live on {@link ServiceListing#commissionPctOverride}
 * which always wins over this table when set.
 */
@Entity
@Table(name = "commission_rate_config", schema = "services",
        uniqueConstraints = @UniqueConstraint(name = "uq_commission_rate_type_tier", columnNames = {"service_type", "tier"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRateConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType;

    @Column(nullable = false, length = 20)
    private String tier;                            // STARTER, PRO, COMMERCIAL

    @Column(name = "commission_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPct;

    @Column(name = "promotion_threshold", nullable = false)
    private Integer promotionThreshold;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
