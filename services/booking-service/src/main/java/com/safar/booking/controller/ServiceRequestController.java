package com.safar.booking.controller;

import com.safar.booking.dto.*;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.service.MaintenanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Guest-facing service request controller for hotel/apartment bookings.
 * Allows guests to request services (water, breakfast, room service, etc.)
 * linked to their active booking.
 */
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final MaintenanceRequestService maintenanceService;

    @PostMapping
    public ResponseEntity<TicketDetailResponse> createRequest(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody CreateMaintenanceRequestDto request) {
        MaintenanceRequest saved = maintenanceService.createServiceRequest(bookingId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.toDetailResponse(saved));
    }

    @GetMapping
    public ResponseEntity<Page<TicketDetailResponse>> getRequests(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<TicketDetailResponse> page = maintenanceService.getBookingRequests(bookingId, status, pageable)
                .map(maintenanceService::toDetailResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<TicketDetailResponse> getRequest(
            @PathVariable UUID bookingId,
            @PathVariable UUID requestId) {
        MaintenanceRequest request = maintenanceService.getRequest(requestId);
        return ResponseEntity.ok(maintenanceService.toDetailResponse(request));
    }

    @PostMapping("/{requestId}/rate")
    public ResponseEntity<TicketDetailResponse> rateRequest(
            @PathVariable UUID bookingId,
            @PathVariable UUID requestId,
            @RequestParam int rating,
            @RequestParam(required = false) String feedback) {
        MaintenanceRequest rated = maintenanceService.rateResolution(requestId, rating, feedback != null ? feedback : "");
        return ResponseEntity.ok(maintenanceService.toDetailResponse(rated));
    }

    @PostMapping("/{requestId}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable UUID bookingId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody TicketCommentDto comment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.addComment(requestId, userId, "GUEST", comment));
    }

    @GetMapping("/{requestId}/comments")
    public ResponseEntity<List<TicketCommentResponse>> getComments(
            @PathVariable UUID bookingId,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(maintenanceService.getComments(requestId));
    }
}
