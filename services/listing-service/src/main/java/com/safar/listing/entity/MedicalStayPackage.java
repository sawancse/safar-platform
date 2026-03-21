package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_stay_packages", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalStayPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "distance_km", precision = 5, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "includes_pickup", nullable = false)
    @Builder.Default
    private Boolean includesPickup = false;

    @Column(name = "includes_translator", nullable = false)
    @Builder.Default
    private Boolean includesTranslator = false;

    @Column(name = "caregiver_friendly", nullable = false)
    @Builder.Default
    private Boolean caregiverFriendly = false;

    @Column(name = "medical_price_paise", nullable = false)
    private Long medicalPricePaise;

    @Column(name = "min_stay_nights", nullable = false)
    @Builder.Default
    private Integer minStayNights = 3;

    @Column(name = "recovery_days")
    @Builder.Default
    private Integer recoveryDays = 7;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
