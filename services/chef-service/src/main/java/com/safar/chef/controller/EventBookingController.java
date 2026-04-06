package com.safar.chef.controller;

import com.safar.chef.dto.CreateEventBookingRequest;
import com.safar.chef.dto.ModifyEventBookingRequest;
import com.safar.chef.entity.EventBooking;
import com.safar.chef.service.EventBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef-events")
@RequiredArgsConstructor
public class EventBookingController {

    private final EventBookingService eventBookingService;

    @GetMapping
    public ResponseEntity<Page<EventBooking>> browse(Pageable pageable) {
        return ResponseEntity.ok(eventBookingService.browseEvents(pageable));
    }

    // ── Admin ─────────────────────────────────────────────────

    @GetMapping("/admin/all")
    public ResponseEntity<Page<EventBooking>> adminListAll(Pageable pageable) {
        return ResponseEntity.ok(eventBookingService.browseEvents(pageable));
    }

    @PostMapping("/admin/{id}/assign")
    public ResponseEntity<EventBooking> adminAssignChef(@PathVariable UUID id,
                                                         @RequestParam UUID chefId) {
        return ResponseEntity.ok(eventBookingService.adminAssignChef(id, chefId));
    }

    @PostMapping
    public ResponseEntity<EventBooking> createEvent(Authentication auth,
                                                     @RequestBody CreateEventBookingRequest req) {
        UUID customerId = auth != null ? UUID.fromString(auth.getName()) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventBookingService.createEventBooking(customerId, req));
    }

    @PostMapping("/{id}/quote")
    public ResponseEntity<EventBooking> quoteEvent(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam Long totalAmountPaise) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.quoteEvent(chefId, id, totalAmountPaise));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<EventBooking> confirmEvent(Authentication auth,
                                                      @PathVariable UUID id) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.confirmEvent(customerId, id));
    }

    @PostMapping("/{id}/advance-paid")
    public ResponseEntity<EventBooking> markAdvancePaid(@PathVariable UUID id) {
        return ResponseEntity.ok(eventBookingService.markAdvancePaid(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<EventBooking> completeEvent(Authentication auth,
                                                       @PathVariable UUID id) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.completeEvent(chefId, id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventBooking> cancelEvent(Authentication auth,
                                                     @PathVariable UUID id,
                                                     @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.cancelEvent(userId, id, reason));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<EventBooking> rateEvent(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @RequestParam int rating,
                                                   @RequestParam(required = false) String comment) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.rateEvent(customerId, id, rating, comment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventBooking> modifyEvent(Authentication auth,
                                                     @PathVariable UUID id,
                                                     @RequestBody ModifyEventBookingRequest req) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.modifyEventBooking(customerId, id, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventBooking>> getMyEvents(Authentication auth) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.getMyEvents(customerId));
    }

    @GetMapping("/chef")
    public ResponseEntity<List<EventBooking>> getChefEvents(Authentication auth) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.getChefEvents(chefId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventBooking> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventBookingService.getEvent(id));
    }
}
