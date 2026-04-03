package com.safar.booking.controller;

import com.safar.booking.dto.CreateMaintenanceRequestDto;
import com.safar.booking.dto.UpdateMaintenanceRequestDto;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.service.MaintenanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<MaintenanceRequest> getRequest(
            @PathVariable UUID tenancyId,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(maintenanceService.getRequest(requestId));
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
}
