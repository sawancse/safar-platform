package com.safar.booking.controller;

import com.safar.booking.dto.CreateRecurringRequest;
import com.safar.booking.entity.RecurringBooking;
import com.safar.booking.service.RecurringBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/recurring")
@RequiredArgsConstructor
public class RecurringBookingController {

    private final RecurringBookingService recurringService;

    @PostMapping
    public ResponseEntity<RecurringBooking> create(Authentication auth,
                                                    @Valid @RequestBody CreateRecurringRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringService.createRecurring(guestId, req));
    }

    @GetMapping
    public ResponseEntity<List<RecurringBooking>> list(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(recurringService.getGuestRecurringBookings(guestId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RecurringBooking> cancel(Authentication auth, @PathVariable UUID id) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(recurringService.cancel(guestId, id));
    }
}
