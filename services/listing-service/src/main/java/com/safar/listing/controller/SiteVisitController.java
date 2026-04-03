package com.safar.listing.controller;

import com.safar.listing.dto.ScheduleVisitRequest;
import com.safar.listing.dto.SiteVisitResponse;
import com.safar.listing.entity.enums.VisitStatus;
import com.safar.listing.service.SiteVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/site-visits")
@RequiredArgsConstructor
public class SiteVisitController {

    private final SiteVisitService visitService;

    @PostMapping
    public ResponseEntity<SiteVisitResponse> schedule(
            @Valid @RequestBody ScheduleVisitRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.schedule(request, userId));
    }

    @GetMapping("/buyer")
    public ResponseEntity<Page<SiteVisitResponse>> getBuyerVisits(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(visitService.getBuyerVisits(userId, pageable));
    }

    @GetMapping("/seller")
    public ResponseEntity<Page<SiteVisitResponse>> getSellerVisits(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(visitService.getSellerVisits(userId, pageable));
    }

    @GetMapping("/buyer/upcoming")
    public ResponseEntity<List<SiteVisitResponse>> getUpcomingBuyer(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(visitService.getUpcomingForBuyer(userId));
    }

    @GetMapping("/seller/upcoming")
    public ResponseEntity<List<SiteVisitResponse>> getUpcomingSeller(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(visitService.getUpcomingForSeller(userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SiteVisitResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam VisitStatus status,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(visitService.updateStatus(id, status, userId));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<SiteVisitResponse> addFeedback(
            @PathVariable UUID id,
            @RequestParam String feedback,
            @RequestParam(required = false) Integer rating,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(visitService.addFeedback(id, feedback, rating, userId));
    }
}
