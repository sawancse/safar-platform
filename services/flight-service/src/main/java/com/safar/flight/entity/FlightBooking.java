package com.safar.flight.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "flight_bookings", schema = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 20)
    private String bookingRef;

    @Column(length = 100)
    private String duffelOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightBookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CabinClass cabinClass;

    @Column(length = 100)
    private String departureCity;

    @Column(length = 5)
    private String departureCityCode;

    @Column(length = 100)
    private String arrivalCity;

    @Column(length = 5)
    private String arrivalCityCode;

    @Column(nullable = false)
    private LocalDate departureDate;

    private LocalDate returnDate;

    @Column(length = 100)
    private String airline;

    @Column(length = 20)
    private String flightNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isInternational = false;

    @Column(columnDefinition = "TEXT")
    private String passengersJson;

    @Column(columnDefinition = "TEXT")
    private String slicesJson;

    @Column(nullable = false)
    private Long totalAmountPaise;

    @Builder.Default
    private Long taxPaise = 0L;

    @Builder.Default
    private Long platformFeePaise = 0L;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 100)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(length = 20)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(length = 200)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 500)
    private String cancellationReason;

    private Instant cancelledAt;

    private Long refundAmountPaise;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
