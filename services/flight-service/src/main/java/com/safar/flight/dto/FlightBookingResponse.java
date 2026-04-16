package com.safar.flight.dto;

import com.safar.flight.entity.CabinClass;
import com.safar.flight.entity.FlightBookingStatus;
import com.safar.flight.entity.TripType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FlightBookingResponse(
        UUID id,
        UUID userId,
        String bookingRef,
        String duffelOrderId,
        FlightBookingStatus status,
        TripType tripType,
        CabinClass cabinClass,
        String departureCity,
        String departureCityCode,
        String arrivalCity,
        String arrivalCityCode,
        LocalDate departureDate,
        LocalDate returnDate,
        String airline,
        String flightNumber,
        Boolean isInternational,
        String passengersJson,
        String slicesJson,
        Long totalAmountPaise,
        Long taxPaise,
        Long platformFeePaise,
        String currency,
        String razorpayOrderId,
        String razorpayPaymentId,
        String paymentStatus,
        String contactEmail,
        String contactPhone,
        String cancellationReason,
        Instant cancelledAt,
        Long refundAmountPaise,
        Instant createdAt,
        Instant updatedAt
) {}
