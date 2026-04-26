package com.safar.booking.entity;

import com.safar.booking.entity.enums.TripIntent;
import com.safar.booking.entity.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Universal Trip — the cross-vertical container that wraps a flight + stay
 * + cab + cook + insurance booking under one trip identity.
 *
 * Created when the user's first vertical of a trip is booked (typically
 * the flight); other verticals are attached as TripLegs as the user adds
 * them via the cross-vertical suggestion engine.
 *
 * Cancel / refund logic:
 *  - Cancel one leg → that leg.status = CANCELLED, trip.status = PARTIAL_CANCEL,
 *    user nudged to cancel siblings (do NOT auto-cascade).
 *  - Cancel whole trip → cascade-cancel all legs via their respective services.
 *  - Bundle discount is one-time at booking; NOT clawed back on partial cancel.
 */
@Entity
@Table(name = "trips", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String tripName;          // auto: "Hyderabad → Tirupati, 28-29 Apr"

    @Column(length = 120)
    private String originCity;

    @Column(length = 120)
    private String destinationCity;

    /** IATA airport code (e.g. 'BLR'). Used by TripIntentEvaluator for DESTINATION/ROUTE rule matching. */
    @Column(length = 5)
    private String originCode;

    @Column(length = 5)
    private String destinationCode;

    /** ISO-3166-1 alpha-2 country code (e.g. 'IN', 'AE', 'SG'). Multi-country ready. */
    @Column(nullable = false, length = 2)
    @Builder.Default
    private String originCountry = "IN";

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String destinationCountry = "IN";

    /** ISO-3166-2 subdivision (e.g. 'IN-KA' for Karnataka, 'AE-DU' for Dubai). */
    @Column(length = 10)
    private String originState;

    @Column(length = 10)
    private String destinationState;

    /** Safar-defined broader bucket for routing rules (e.g. 'SOUTH_INDIA', 'GULF', 'SE_ASIA'). */
    @Column(length = 30)
    private String originRegion;

    @Column(length = 30)
    private String destinationRegion;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TripIntent tripIntent = TripIntent.UNCLASSIFIED;

    @Column(nullable = false)
    @Builder.Default
    private Integer paxCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Long bundleDiscountPaise = 0L;

    /** True if user explicitly set the intent (rather than rule-based inference). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean intentOverriddenByUser = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TripLeg> legs = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
