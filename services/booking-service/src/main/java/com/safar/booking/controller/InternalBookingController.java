package com.safar.booking.controller;

import com.safar.booking.entity.Booking;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.service.ListingServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/bookings")
@RequiredArgsConstructor
@Slf4j
public class InternalBookingController {

    private final BookingRepository bookingRepository;
    private final ListingServiceClient listingClient;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long total = bookingRepository.count();
        return ResponseEntity.ok(Map.of("count", total));
    }

    @PostMapping("/backfill-listing-details")
    public ResponseEntity<Map<String, Object>> backfillListingDetails() {
        List<Booking> bookings = bookingRepository.findAll();
        int updated = 0;
        for (Booking b : bookings) {
            if (b.getListingCity() != null && b.getListingType() != null) continue;
            try {
                if (b.getListingCity() == null) b.setListingCity(listingClient.getCity(b.getListingId()));
                if (b.getListingType() == null) b.setListingType(listingClient.getListingType(b.getListingId()));
                if (b.getListingPhotoUrl() == null) b.setListingPhotoUrl(listingClient.getListingPhotoUrl(b.getListingId()));
                if (b.getHostName() == null) b.setHostName(listingClient.getHostName(b.getListingId()));
                if (b.getListingAddress() == null) b.setListingAddress(listingClient.getListingAddress(b.getListingId()));
                bookingRepository.save(b);
                updated++;
            } catch (Exception e) {
                log.warn("Backfill failed for booking {}: {}", b.getId(), e.getMessage());
            }
        }
        log.info("Backfilled listing details for {} bookings", updated);
        return ResponseEntity.ok(Map.of("total", bookings.size(), "updated", updated));
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
