package com.safar.flight.controller;

import com.safar.flight.dto.*;
import com.safar.flight.service.AmadeusFlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Slf4j
public class FlightController {

    private final AmadeusFlightService flightService;

    /**
     * Search flights — public endpoint.
     */
    @GetMapping("/search")
    public ResponseEntity<FlightSearchResponse> searchFlights(@Valid FlightSearchRequest request) {
        log.info("Flight search: {} -> {} on {}", request.origin(), request.destination(), request.departureDate());
        FlightSearchResponse response = flightService.searchFlights(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a flight booking — requires authentication.
     */
    @PostMapping("/book")
    public ResponseEntity<FlightBookingResponse> createBooking(
            Authentication auth,
            @Valid @RequestBody CreateFlightBookingRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        log.info("Creating flight booking for user {}", userId);
        FlightBookingResponse response = flightService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirm payment for a booking — requires authentication.
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<FlightBookingResponse> confirmPayment(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody Map<String, String> paymentDetails) {
        UUID userId = UUID.fromString(auth.getName());
        String razorpayOrderId = paymentDetails.get("razorpayOrderId");
        String razorpayPaymentId = paymentDetails.get("razorpayPaymentId");
        log.info("Confirming payment for booking {} user {}", id, userId);
        FlightBookingResponse response = flightService.confirmPayment(userId, id, razorpayOrderId, razorpayPaymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a booking — requires authentication.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<FlightBookingResponse> cancelBooking(
            Authentication auth,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        log.info("Cancelling flight booking {} for user {}", id, userId);
        FlightBookingResponse response = flightService.cancelBooking(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's bookings — requires authentication.
     */
    @GetMapping("/my")
    public ResponseEntity<Page<FlightBookingResponse>> getMyBookings(
            Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        UUID userId = UUID.fromString(auth.getName());
        Page<FlightBookingResponse> bookings = flightService.getMyBookings(userId, pageable);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get a single booking by ID — requires authentication.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlightBookingResponse> getBooking(
            Authentication auth,
            @PathVariable UUID id) {
        FlightBookingResponse response = flightService.getBooking(id);
        return ResponseEntity.ok(response);
    }
}
