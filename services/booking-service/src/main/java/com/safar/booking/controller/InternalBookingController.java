package com.safar.booking.controller;

import com.safar.booking.dto.BookingResponse;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.BookingRoomSelection;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.BookingRoomSelectionRepository;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.service.BookingService;
import com.safar.booking.service.ListingServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final BookingRoomSelectionRepository roomSelectionRepository;
    private final PgTenancyRepository pgTenancyRepository;
    private final BookingService bookingService;
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
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        // Return the full BookingResponse DTO so notification-service gets
        // checkIn/checkOut/guests/listingTitle/amounts for email templates.
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    /**
     * Reconcile helper: returns live bed consumption per room type for a listing,
     * derived from active bookings (CONFIRMED/CHECKED_IN) and active tenancies
     * (ACTIVE/NOTICE_PERIOD). listing-service uses this to reset `occupied_beds`
     * when the aggregate drifts out of sync.
     *
     * The caller (listing-service) knows bedsPerRoom and sharingType, so this
     * endpoint just returns raw "booking units" per room type — listing-service
     * applies its own semantics.
     */
    @GetMapping("/active-units-by-listing/{listingId}")
    public ResponseEntity<Map<String, Object>> activeUnitsByListing(@PathVariable UUID listingId) {
        Map<String, Long> byRoomTypeWhole = new HashMap<>();  // whole-room bookings (PRIVATE)
        Map<String, Long> byRoomTypeBeds = new HashMap<>();   // bed-level units (shared + tenancies)

        // Active bookings — loop statuses
        List<Booking> active = bookingRepository.findByListingIdAndStatusIn(
                listingId, List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN));
        for (Booking b : active) {
            List<BookingRoomSelection> sels = roomSelectionRepository.findByBookingId(b.getId());
            if (!sels.isEmpty()) {
                for (BookingRoomSelection s : sels) {
                    // `count` represents the booking unit. listing-service applies
                    // per-type semantics when reconciling. We just report the raw units.
                    byRoomTypeBeds.merge(s.getRoomTypeId().toString(), (long) s.getCount(), Long::sum);
                }
            } else if (b.getRoomTypeId() != null) {
                int rooms = b.getRoomsCount() != null ? b.getRoomsCount() : 1;
                byRoomTypeWhole.merge(b.getRoomTypeId().toString(), (long) rooms, Long::sum);
            }
        }

        // Active tenancies — 1 bed each
        List<PgTenancy> tenancies = pgTenancyRepository.findAll().stream()
                .filter(t -> t.getListingId() != null && t.getListingId().equals(listingId))
                .filter(t -> t.getStatus() == TenancyStatus.ACTIVE
                        || t.getStatus() == TenancyStatus.NOTICE_PERIOD)
                .toList();
        for (PgTenancy t : tenancies) {
            if (t.getRoomTypeId() == null) continue;
            byRoomTypeBeds.merge(t.getRoomTypeId().toString(), 1L, Long::sum);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("listingId", listingId);
        out.put("unitsByRoomType", byRoomTypeBeds);           // shared units + tenancies
        out.put("wholeRoomUnitsByRoomType", byRoomTypeWhole); // PRIVATE room bookings
        out.put("activeBookingCount", active.size());
        out.put("activeTenancyCount", tenancies.size());
        return ResponseEntity.ok(out);
    }
}
