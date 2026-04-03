package com.safar.listing.entity;

import com.safar.listing.entity.enums.FinancingType;
import com.safar.listing.entity.enums.PossessionTimeline;
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
@Table(name = "buyer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> preferredCities;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> preferredLocalities;

    private Long budgetMinPaise;

    private Long budgetMaxPaise;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> preferredBhk;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> preferredTypes;

    @Enumerated(EnumType.STRING)
    private FinancingType financingType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PossessionTimeline possessionTimeline = PossessionTimeline.FLEXIBLE;

    @Builder.Default
    private Boolean alertsEnabled = true;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
