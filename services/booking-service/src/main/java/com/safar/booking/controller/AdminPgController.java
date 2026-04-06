package com.safar.booking.controller;

import com.safar.booking.dto.*;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.entity.TenancySettlement;
import com.safar.booking.entity.enums.MaintenanceStatus;
import com.safar.booking.entity.enums.SettlementStatus;
import com.safar.booking.service.MaintenanceRequestService;
import com.safar.booking.service.TenancySettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pg")
@RequiredArgsConstructor
public class AdminPgController {

    private final MaintenanceRequestService maintenanceService;
    private final TenancySettlementService settlementService;

    // ── Tickets ──────────────────────────────────────────────

    @GetMapping("/tickets")
    public ResponseEntity<Page<MaintenanceRequest>> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean slaBreached,
            Pageable pageable) {
        if (Boolean.TRUE.equals(slaBreached)) {
            return ResponseEntity.ok(maintenanceService.getSlaBreachedTickets(pageable));
        }
        List<MaintenanceStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = Arrays.stream(status.split(","))
                    .map(MaintenanceStatus::valueOf)
                    .toList();
        }
        return ResponseEntity.ok(maintenanceService.getAllTickets(statuses, pageable));
    }

    @GetMapping("/tickets/{requestId}")
    public ResponseEntity<TicketDetailResponse> getTicket(@PathVariable UUID requestId) {
        MaintenanceRequest request = maintenanceService.getRequest(requestId);
        return ResponseEntity.ok(maintenanceService.toDetailResponse(request));
    }

    @PutMapping("/tickets/{requestId}")
    public ResponseEntity<MaintenanceRequest> updateTicket(
            @PathVariable UUID requestId,
            @RequestBody UpdateMaintenanceRequestDto request) {
        return ResponseEntity.ok(maintenanceService.updateRequest(requestId, request));
    }

    @PostMapping("/tickets/{requestId}/comments")
    public ResponseEntity<TicketCommentResponse> addTicketComment(
            @PathVariable UUID requestId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody TicketCommentDto comment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.addComment(requestId, userId, "ADMIN", comment));
    }

    @GetMapping("/tickets/stats")
    public ResponseEntity<TicketStatsResponse> getGlobalTicketStats(
            @RequestParam(required = false) UUID listingId) {
        if (listingId != null) {
            return ResponseEntity.ok(maintenanceService.getTicketStats(listingId));
        }
        // For global stats without listingId, return empty structure
        return ResponseEntity.ok(new TicketStatsResponse(0, 0, 0, 0, 0, null, 100.0, Map.of()));
    }

    // ── Settlements ──────────────────────────────────────────

    @GetMapping("/settlements")
    public ResponseEntity<Page<SettlementResponse>> getAllSettlements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean overdue,
            Pageable pageable) {
        Page<TenancySettlement> settlements;
        if (Boolean.TRUE.equals(overdue)) {
            settlements = settlementService.getOverdueSettlements(pageable);
        } else if (status != null && !status.isEmpty()) {
            List<SettlementStatus> statuses = Arrays.stream(status.split(","))
                    .map(SettlementStatus::valueOf)
                    .toList();
            settlements = settlementService.getSettlementsByStatus(statuses, pageable);
        } else {
            settlements = settlementService.getAllSettlements(pageable);
        }
        return ResponseEntity.ok(settlements.map(settlementService::toResponse));
    }

    @GetMapping("/settlements/{settlementId}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable UUID settlementId) {
        TenancySettlement settlement = settlementService.getById(settlementId);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }

    @PostMapping("/settlements/{settlementId}/resolve-dispute")
    public ResponseEntity<Void> resolveDispute(
            @PathVariable UUID settlementId,
            @RequestBody AdminResolveDisputeRequest request) {
        settlementService.adminResolveDispute(settlementId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settlements/{settlementId}/override")
    public ResponseEntity<SettlementResponse> overrideSettlement(
            @PathVariable UUID settlementId,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestBody AdminOverrideRequest request) {
        TenancySettlement settlement = settlementService.adminOverrideSettlement(settlementId, adminId, request);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }

    @GetMapping("/settlements/overdue")
    public ResponseEntity<Page<SettlementResponse>> getOverdueSettlements(Pageable pageable) {
        Page<TenancySettlement> settlements = settlementService.getOverdueSettlements(pageable);
        return ResponseEntity.ok(settlements.map(settlementService::toResponse));
    }

    @GetMapping("/settlements/stats")
    public ResponseEntity<Map<String, Object>> getSettlementStats() {
        return ResponseEntity.ok(settlementService.getSettlementStats());
    }
}
