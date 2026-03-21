package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_anywhere_stays", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAnywhereStay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Integer nights;

    @Column(name = "listing_price", nullable = false)
    private Long listingPrice;

    @Column(name = "covered_paise", nullable = false)
    private Long coveredPaise;

    @Column(name = "guest_topup", nullable = false)
    @Builder.Default
    private Long guestTopup = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
