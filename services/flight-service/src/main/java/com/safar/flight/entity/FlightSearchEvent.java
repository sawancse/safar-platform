package com.safar.flight.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Captured on every search hit. Drives the abandoned-search recovery
 * pipeline: AbandonedSearchDetector @Scheduled job scans for
 * {@code suppressed=false AND reminders_sent < 3} rows whose age matches
 * a pulse window (1h / 6h / 24h), and publishes Kafka events for
 * notification-service to fan out via Push → WhatsApp → Email.
 */
@Entity
@Table(name = "flight_search_events", schema = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nullable — anonymous searches are tracked by deviceId until login promotes them. */
    private UUID userId;

    /** Cookie / localStorage UUID for anonymous identity. */
    @Column(length = 80)
    private String deviceId;

    @Column(nullable = false, length = 5)
    private String origin;

    @Column(nullable = false, length = 5)
    private String destination;

    @Column(nullable = false)
    private LocalDate departureDate;

    private LocalDate returnDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer paxCount = 1;

    @Column(length = 20)
    private String cabinClass;

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String originCountry = "IN";

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String destinationCountry = "IN";

    /** Cheapest fare seen at search time — basis for the fare-trend signal in the reminder. */
    private Long cheapestFarePaise;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false)
    @Builder.Default
    private Integer remindersSent = 0;

    private Instant lastReminderAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean suppressed = false;

    /** BOOKED / DATE_PASSED / OPT_OUT / MAX_REMINDERS / EXPIRED. */
    @Column(length = 40)
    private String suppressionReason;

    @Column(length = 200)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
