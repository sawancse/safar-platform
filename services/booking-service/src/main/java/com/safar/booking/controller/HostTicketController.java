package com.safar.booking.controller;

import com.safar.booking.dto.TicketCommentDto;
import com.safar.booking.dto.TicketCommentResponse;
import com.safar.booking.dto.TicketDetailResponse;
import com.safar.booking.dto.TicketStatsResponse;
import com.safar.booking.dto.UpdateMaintenanceRequestDto;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.service.MaintenanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/tickets")
@RequiredArgsConstructor
public class HostTicketController {

    private final MaintenanceRequestService maintenanceService;

    @GetMapping
    public ResponseEntity<Page<MaintenanceRequest>> getTickets(
            @PathVariable UUID listingId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.getTicketsByListing(listingId, status, pageable));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<TicketDetailResponse> getTicket(
            @PathVariable UUID listingId,
            @PathVariable UUID requestId) {
        MaintenanceRequest request = maintenanceService.getRequest(requestId);
        return ResponseEntity.ok(maintenanceService.toDetailResponse(request));
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<MaintenanceRequest> updateTicket(
            @PathVariable UUID listingId,
            @PathVariable UUID requestId,
            @RequestBody UpdateMaintenanceRequestDto request) {
        return ResponseEntity.ok(maintenanceService.updateRequest(requestId, request));
    }

    @PostMapping("/{requestId}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable UUID listingId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody TicketCommentDto comment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.addComment(requestId, userId, "HOST", comment));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsResponse> getStats(@PathVariable UUID listingId) {
        return ResponseEntity.ok(maintenanceService.getTicketStats(listingId));
    }
}
