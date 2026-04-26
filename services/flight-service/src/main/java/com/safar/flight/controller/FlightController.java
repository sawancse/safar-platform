package com.safar.flight.controller;

import com.safar.flight.dto.*;
import com.safar.flight.service.FlightBookingService;
import com.safar.flight.service.FlightSearchService;
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

    private final FlightSearchService searchService;
    private final FlightBookingService bookingService;

    @GetMapping("/search")
    public ResponseEntity<FlightSearchResponse> searchFlights(@Valid FlightSearchRequest request) {
        log.info("Flight search: {} -> {} on {}", request.origin(), request.destination(), request.departureDate());
        return ResponseEntity.ok(searchService.search(request));
    }

    @PostMapping("/book")
    public ResponseEntity<FlightBookingResponse> createBooking(
            Authentication auth,
            @Valid @RequestBody CreateFlightBookingRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        log.info("Creating flight booking for user {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(userId, request));
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<FlightBookingResponse> confirmPayment(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody Map<String, String> paymentDetails) {
        UUID userId = UUID.fromString(auth.getName());
        String razorpayOrderId = paymentDetails.get("razorpayOrderId");
        String razorpayPaymentId = paymentDetails.get("razorpayPaymentId");
        log.info("Confirming payment for booking {} user {}", id, userId);
        return ResponseEntity.ok(bookingService.confirmPayment(userId, id, razorpayOrderId, razorpayPaymentId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FlightBookingResponse> cancelBooking(
            Authentication auth,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        log.info("Cancelling flight booking {} for user {}", id, userId);
        return ResponseEntity.ok(bookingService.cancelBooking(userId, id));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<FlightBookingResponse>> getMyBookings(
            Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getMyBookings(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightBookingResponse> getBooking(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }
}
