package com.safar.listing.controller;

import com.safar.listing.dto.BulkAvailabilityRequest;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.RoomType;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.RoomTypeRepository;
import com.safar.listing.service.AvailabilityService;
import com.safar.listing.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/listings")
@RequiredArgsConstructor
@Slf4j
public class InternalListingController {

    private final ListingRepository listingRepository;
    private final AvailabilityService availabilityService;
    private final RoomTypeService roomTypeService;
    private final RoomTypeRepository roomTypeRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long total = listingRepository.count();
        long pending = listingRepository.findByStatus(ListingStatus.PENDING_VERIFICATION).size();
        long verified = listingRepository.findByStatus(ListingStatus.VERIFIED).size();
        return ResponseEntity.ok(Map.of(
                "total", total,
                "pending", pending,
                "verified", verified
        ));
    }

    @PostMapping("/{listingId}/block-dates")
    public ResponseEntity<Void> blockDates(@PathVariable UUID listingId,
                                           @RequestBody Map<String, String> body) {
        LocalDate from = LocalDate.parse(body.get("fromDate"));
        LocalDate to = LocalDate.parse(body.get("toDate"));
        String source = body.getOrDefault("source", "BOOKING");
        availabilityService.bulkUpsertAvailabilityWithSource(listingId,
                new BulkAvailabilityRequest(from, to, false, null, null, null), source);
        log.info("Blocked dates {}-{} for listing {} (source: {})", from, to, listingId, source);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{listingId}/unblock-dates")
    public ResponseEntity<Void> unblockDates(@PathVariable UUID listingId,
                                             @RequestBody Map<String, String> body) {
        LocalDate from = LocalDate.parse(body.get("fromDate"));
        LocalDate to = LocalDate.parse(body.get("toDate"));
        availabilityService.bulkUpsertAvailability(listingId,
                new BulkAvailabilityRequest(from, to, true, null, null, null));
        log.info("Unblocked dates {}-{} for listing {}", from, to, listingId);
        return ResponseEntity.ok().build();
    }

    /**
     * Check if listing is available for given dates.
     * Single-room: checks calendar (isAvailable per date).
     * Multi-room (hotel/guesthouse): checks room-type inventory (rooms left > 0).
     */
    @GetMapping("/{listingId}/check-availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable UUID listingId,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        boolean statusOk = listing.getStatus() == ListingStatus.VERIFIED;
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        // Calculate actual totalRooms: use listing field OR sum of room type counts
        var roomTypes = roomTypeRepository.findByListingIdOrderBySortOrder(listingId);
        int totalRooms;
        if (listing.getTotalRooms() != null && listing.getTotalRooms() > 1) {
            totalRooms = listing.getTotalRooms();
        } else if (!roomTypes.isEmpty()) {
            totalRooms = roomTypes.stream().mapToInt(rt -> rt.getCount() != null ? rt.getCount() : 0).sum();
        } else {
            totalRooms = 1;
        }

        boolean datesAvailable;
        if (statusOk && totalRooms > 1) {
            // Multi-room property (hotel/guesthouse/PG):
            if (roomTypes.isEmpty()) {
                // No room types defined → property has totalRooms > 1 but no room types
                // Treat as available (host manages rooms manually, calendar blocks ignored)
                datesAvailable = true;
                log.debug("Multi-room listing {} has no room types — treating as available", listingId);
            } else {
                // Check if ANY room type has >= 1 room available for the full date range
                datesAvailable = false;
                for (var rt : roomTypes) {
                    int minAvail = roomTypeService.computeMinAvailablePublic(
                            rt.getId(), checkInDate, checkOutDate, rt.getCount());
                    if (minAvail > 0) {
                        datesAvailable = true;
                        break;
                    }
                }
            }
        } else {
            // Single-room: use calendar availability
            datesAvailable = statusOk && availabilityService.areDatesAvailable(listingId, checkInDate, checkOutDate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", statusOk && datesAvailable);
        result.put("status", listing.getStatus().name());
        result.put("maxGuests", listing.getMaxGuests());
        result.put("totalRooms", totalRooms);
        result.put("petFriendly", listing.getPetFriendly());
        result.put("maxPets", listing.getMaxPets());
        return ResponseEntity.ok(result);
    }

    /**
     * Check room type availability for date range.
     * Returns minimum available count across all dates.
     */
    @GetMapping("/room-types/{roomTypeId}/check-availability")
    public ResponseEntity<Map<String, Object>> checkRoomTypeAvailability(
            @PathVariable UUID roomTypeId,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        int minAvailable = roomTypeService.computeMinAvailablePublic(
                roomTypeId, LocalDate.parse(checkIn), LocalDate.parse(checkOut), roomType.getCount());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomTypeId", roomTypeId);
        result.put("minAvailable", minAvailable);
        result.put("maxGuests", roomType.getMaxGuests());
        result.put("totalCount", roomType.getCount());
        return ResponseEntity.ok(result);
    }

    /**
     * Reset room-type availability AND unblock calendar dates.
     * No auth required (internal endpoint).
     */
    @PostMapping("/{listingId}/reset-all-availability")
    public ResponseEntity<Map<String, Object>> resetAllAvailability(
            @PathVariable UUID listingId,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        // 1. Unblock calendar dates
        availabilityService.bulkUpsertAvailability(listingId,
                new BulkAvailabilityRequest(fromDate, toDate, true, null, null, null));

        // 2. Reset all room types to full capacity
        var roomTypes = roomTypeRepository.findByListingIdOrderBySortOrder(listingId);
        int resetCount = 0;
        for (var rt : roomTypes) {
            roomTypeService.resetAvailability(rt.getId(), fromDate, toDate);
            resetCount++;
        }

        log.info("Reset all availability for listing {} from {} to {} ({} room types)",
                listingId, from, to, resetCount);

        return ResponseEntity.ok(Map.of(
                "listingId", listingId.toString(),
                "from", from,
                "to", to,
                "calendarUnblocked", true,
                "roomTypesReset", resetCount
        ));
    }
}
