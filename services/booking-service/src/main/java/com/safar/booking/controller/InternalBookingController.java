package com.safar.booking.controller;

import com.safar.booking.entity.Booking;
import com.safar.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingRepository bookingRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long total = bookingRepository.count();
        return ResponseEntity.ok(Map.of("count", total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBooking(@PathVariable UUID id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + id));
        return ResponseEntity.ok(Map.of(
                "id", b.getId(),
                "bookingRef", b.getBookingRef(),
                "guestId", b.getGuestId(),
                "hostId", b.getHostId(),
                "listingId", b.getListingId(),
                "guestEmail", b.getGuestEmail() != null ? b.getGuestEmail() : "",
                "guestFirstName", b.getGuestFirstName() != null ? b.getGuestFirstName() : "",
                "guestLastName", b.getGuestLastName() != null ? b.getGuestLastName() : "",
                "status", b.getStatus().name()
        ));
    }
}
