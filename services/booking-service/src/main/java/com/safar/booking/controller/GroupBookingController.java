package com.safar.booking.controller;

import com.safar.booking.dto.BookingResponse;
import com.safar.booking.dto.GroupBookingRequest;
import com.safar.booking.dto.GroupBookingResult;
import com.safar.booking.service.GroupBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/group")
@RequiredArgsConstructor
public class GroupBookingController {

    private final GroupBookingService groupBookingService;

    @PostMapping
    public ResponseEntity<GroupBookingResult> create(Authentication auth,
                                                      @Valid @RequestBody GroupBookingRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupBookingService.createGroupBooking(guestId, req));
    }

    @GetMapping("/{groupBookingId}")
    public ResponseEntity<List<BookingResponse>> get(@PathVariable UUID groupBookingId) {
        return ResponseEntity.ok(groupBookingService.getGroupBookings(groupBookingId));
    }
}
