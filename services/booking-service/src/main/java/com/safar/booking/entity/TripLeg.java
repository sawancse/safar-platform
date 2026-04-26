package com.safar.booking.entity;

import com.safar.booking.entity.enums.LegStatus;
import com.safar.booking.entity.enums.LegType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One leg of a Trip — references the actual booking row in the originating
 * service (flight-service / listing-service / chef-service / etc.).
 *
 * We do NOT use a database FK across services (different schemas, no
 * cross-service constraints). Consistency is enforced at the application
 * layer via the daily reconciliation job.
 */
@Entity
@Table(name = "trip_legs", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LegType legType;

    /** ID of the booking row in the originating service. */
    @Column(nullable = false)
    private UUID externalBookingId;

    /** Which microservice owns the underlying booking — used for routing cancel calls. */
    @Column(nullable = false, length = 40)
    private String externalService;          // 'flight-service', 'listing-service', 'chef-service', ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LegStatus status = LegStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer legOrder = 0;            // chronological order within the trip

    private Long amountPaise;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    private Long refundAmountPaise;

    private Instant cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
