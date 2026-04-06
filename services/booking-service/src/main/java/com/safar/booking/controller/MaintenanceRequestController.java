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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pg-tenancies/{tenancyId}/maintenance")
@RequiredArgsConstructor
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceRequest> createRequest(
            @PathVariable UUID tenancyId,
            @RequestBody CreateMaintenanceRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.createRequest(tenancyId, request));
    }

    @GetMapping
    public ResponseEntity<Page<MaintenanceRequest>> getRequests(
            @PathVariable UUID tenancyId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.getRequests(tenancyId, status, pageable));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<TicketDetailResponse> getRequest(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId) {
        MaintenanceRequest request = maintenanceService.getRequest(requestId);
        return ResponseEntity.ok(maintenanceService.toDetailResponse(request));
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<MaintenanceRequest> updateRequest(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId,
            @RequestBody UpdateMaintenanceRequestDto request) {
        return ResponseEntity.ok(maintenanceService.updateRequest(requestId, request));
    }

    @PostMapping("/{requestId}/rate")
    public ResponseEntity<MaintenanceRequest> rateRequest(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId,
            @RequestBody Map<String, Object> body) {
        int rating = ((Number) body.get("rating")).intValue();
        String feedback = (String) body.getOrDefault("feedback", "");
        return ResponseEntity.ok(maintenanceService.rateResolution(requestId, rating, feedback));
    }

    // ── New endpoints ──────────────────────────────────────────

    @PostMapping("/{requestId}/reopen")
    public ResponseEntity<TicketDetailResponse> reopenTicket(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId,
            @RequestBody ReopenTicketRequest request) {
        MaintenanceRequest ticket = maintenanceService.reopenTicket(requestId, request.reason());
        return ResponseEntity.ok(maintenanceService.toDetailResponse(ticket));
    }

    @PostMapping("/{requestId}/close")
    public ResponseEntity<TicketDetailResponse> closeTicket(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId) {
        MaintenanceRequest ticket = maintenanceService.closeTicket(requestId);
        return ResponseEntity.ok(maintenanceService.toDetailResponse(ticket));
    }

    @PostMapping("/{requestId}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody TicketCommentDto comment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.addComment(requestId, userId, "TENANT", comment));
    }

    @GetMapping("/{requestId}/comments")
    public ResponseEntity<List<TicketCommentResponse>> getComments(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(maintenanceService.getComments(requestId));
    }
}
