package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "experience_bookings", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "experience_id", nullable = false)
    private UUID experienceId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "property_booking_id")
    private UUID propertyBookingId;

    @Column(name = "num_guests", nullable = false)
    @Builder.Default
    private Integer numGuests = 1;

    @Column(name = "total_paise", nullable = false)
    private Long totalPaise;

    @Column(name = "platform_fee_paise", nullable = false)
    private Long platformFeePaise;

    @Column(name = "host_payout_paise", nullable = false)
    private Long hostPayoutPaise;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(nullable = false, unique = true, length = 20)
    private String ref;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
