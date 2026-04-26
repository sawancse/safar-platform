package com.safar.booking.entity;

import com.safar.booking.entity.enums.TripIntent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the cross-vertical suggestion engine's rule table.
 *
 * Rules fire in priority order (lowest = highest priority). When a Trip
 * matches multiple rules at the same priority, the most-specific match
 * wins (ROUTE > DESTINATION > DATE > GROUP > HISTORY); when multiple
 * rules at the same priority all match, their suggested_verticals are
 * UNION'd.
 *
 * The {@code triggerValue} column is JSONB — its shape varies by
 * triggerType. See V46 migration for the seed examples.
 */
@Entity
@Table(name = "trip_intent_rules", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripIntentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String ruleName;

    @Column(nullable = false)
    private Integer priority;       // 10 = highest, 99 = fallback

    @Column(nullable = false, length = 20)
    private String triggerType;     // DESTINATION / ROUTE / DATE / GROUP / HISTORY / COMPOUND / FALLBACK

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String triggerValue;    // JSON string — shape varies by triggerType

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripIntent inferredIntent;

    /**
     * Stored as PostgreSQL TEXT[]. Hibernate maps to String[]; we store
     * LegType names as strings to avoid coupling the rule table to enum ordinals.
     */
    @Column(name = "suggested_verticals", nullable = false, columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] suggestedVerticals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String verticalFilters; // optional JSON, e.g. {"COOK":{"diet":"sattvik"}}

    @Column(name = "applies_to_country", nullable = false, columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private String[] appliesToCountry = new String[]{"IN"};

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    private UUID createdByUserId;
}
