package com.safar.user.controller;

import com.safar.user.dto.GuestSubscriptionDto;
import com.safar.user.service.GuestSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guests/me/subscription")
@RequiredArgsConstructor
public class GuestSubscriptionController {

    private final GuestSubscriptionService service;

    @PostMapping("/start")
    public ResponseEntity<GuestSubscriptionDto> start(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.subscribe(guestId));
    }

    @GetMapping
    public ResponseEntity<GuestSubscriptionDto> get(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.getSubscription(guestId));
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<GuestSubscriptionDto> cancel(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.cancel(guestId));
    }
}
