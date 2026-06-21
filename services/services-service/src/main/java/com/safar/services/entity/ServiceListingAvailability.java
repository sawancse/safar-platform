package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Calendar row for a service listing (table created in V23). DAY_GRAIN listings
 * leave start/end time null; SLOT_GRAIN listings set a window. A missing row for
 * a date means the vendor is implicitly available (see
 * ServiceListingRepository.findVerifiedAvailableOn).
 */
@Entity
@Table(name = "service_listing_availability", schema = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceListingAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_listing_id", nullable = false)
    private UUID serviceListingId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;        // null for DAY_GRAIN

    @Column(name = "end_time")
    private LocalTime endTime;          // null for DAY_GRAIN

    @Column(nullable = false, length = 20)
    private String status;              // AVAILABLE, BOOKED, BLACKOUT, HIGH_DEMAND

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
