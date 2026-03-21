package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "availability", schema = "listings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"listing_id", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "price_override_paise")
    private Long priceOverridePaise;

    @Column(name = "min_stay_nights", nullable = false)
    @Builder.Default
    private Integer minStayNights = 1;

    @Column(name = "max_stay_nights")
    private Integer maxStayNights;

    @Column(name = "source", length = 50)
    private String source; // MANUAL, ICAL:<feedId>, BOOKING, CHANNEL_MANAGER

    @Column(name = "external_booking_ref")
    private String externalBookingRef;

    @Version
    private Long version;
}
