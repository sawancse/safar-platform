package com.safar.booking.entity;

import com.safar.booking.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_anywhere_subscriptions", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAnywhereSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "guest_id", nullable = false, unique = true)
    private UUID guestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "nights_used_this_month", nullable = false)
    @Builder.Default
    private Integer nightsUsedThisMonth = 0;

    @Column(name = "max_nights_per_month", nullable = false)
    @Builder.Default
    private Integer maxNightsPerMonth = 30;

    @Column(name = "max_covered_paise", nullable = false)
    @Builder.Default
    private Long maxCoveredPaise = 300_000L;

    @Column(name = "current_stay_id")
    private UUID currentStayId;

    @Column(name = "started_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime startedAt;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
}
