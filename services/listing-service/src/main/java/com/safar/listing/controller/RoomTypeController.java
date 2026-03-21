package com.safar.listing.controller;

import com.safar.listing.dto.RoomTypeRequest;
import com.safar.listing.dto.RoomTypeResponse;
import com.safar.listing.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping
    public ResponseEntity<RoomTypeResponse> create(
            Authentication auth,
            @PathVariable UUID listingId,
            @Valid @RequestBody RoomTypeRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomTypeService.createRoomType(listingId, hostId, req));
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeResponse>> list(@PathVariable UUID listingId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypes(listingId));
    }

    @PutMapping("/{roomTypeId}")
    public ResponseEntity<RoomTypeResponse> update(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody RoomTypeRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(roomTypeService.updateRoomType(listingId, roomTypeId, hostId, req));
    }

    @DeleteMapping("/{roomTypeId}")
    public ResponseEntity<Void> delete(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId) {
        UUID hostId = UUID.fromString(auth.getName());
        roomTypeService.deleteRoomType(listingId, roomTypeId, hostId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomTypeResponse>> available(
            @PathVariable UUID listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(roomTypeService.getAvailableRoomTypes(listingId, checkIn, checkOut));
    }

    /**
     * Reset room-type availability to full capacity for a date range.
     */
    @PostMapping("/{roomTypeId}/reset-availability")
    public ResponseEntity<String> resetAvailability(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        roomTypeService.resetAvailability(roomTypeId, from, to);
        return ResponseEntity.ok("Availability reset to full capacity for " + from + " to " + to);
    }

    /**
     * Upload/set photos for a room type (Booking.com style).
     * Accepts primaryPhotoUrl + up to 5 gallery URLs.
     */
    @PutMapping("/{roomTypeId}/photos")
    public ResponseEntity<RoomTypeResponse> updatePhotos(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @RequestBody java.util.Map<String, Object> body) {
        UUID hostId = UUID.fromString(auth.getName());
        String primaryUrl = (String) body.get("primaryPhotoUrl");
        @SuppressWarnings("unchecked")
        List<String> photoUrls = (List<String>) body.get("photoUrls");
        return ResponseEntity.ok(roomTypeService.updatePhotos(listingId, roomTypeId, hostId, primaryUrl, photoUrls));
    }
}
