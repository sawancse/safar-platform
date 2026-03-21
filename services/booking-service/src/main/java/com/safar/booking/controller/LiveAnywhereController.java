package com.safar.booking.controller;

import com.safar.booking.dto.LiveAnywhereBookRequest;
import com.safar.booking.entity.LiveAnywhereStay;
import com.safar.booking.entity.LiveAnywhereSubscription;
import com.safar.booking.service.LiveAnywhereService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live-anywhere")
@RequiredArgsConstructor
public class LiveAnywhereController {

    private final LiveAnywhereService liveAnywhereService;

    @PostMapping("/subscribe")
    public ResponseEntity<LiveAnywhereSubscription> subscribe(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(liveAnywhereService.subscribe(guestId));
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<LiveAnywhereSubscription> cancel(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(liveAnywhereService.cancel(guestId));
    }

    @GetMapping("/subscription")
    public ResponseEntity<LiveAnywhereSubscription> getSubscription(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(liveAnywhereService.getSubscription(guestId));
    }

    @PostMapping("/book")
    public ResponseEntity<LiveAnywhereStay> book(Authentication auth,
                                                  @Valid @RequestBody LiveAnywhereBookRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(liveAnywhereService.bookWithSubscription(
                        guestId, req.listingId(), req.listingPricePerNightPaise(), req.nights()));
    }

    @GetMapping("/stays")
    public ResponseEntity<List<LiveAnywhereStay>> getStays(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(liveAnywhereService.getStays(guestId));
    }
}
