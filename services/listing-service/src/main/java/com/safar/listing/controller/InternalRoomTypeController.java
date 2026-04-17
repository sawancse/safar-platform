package com.safar.listing.controller;

import com.safar.listing.dto.RoomTypeAvailabilityRequest;
import com.safar.listing.dto.RoomTypeInclusionResponse;
import com.safar.listing.dto.RoomTypeResponse;
import com.safar.listing.service.RoomTypeInclusionService;
import com.safar.listing.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/room-types")
@RequiredArgsConstructor
public class InternalRoomTypeController {

    private final RoomTypeService roomTypeService;
    private final RoomTypeInclusionService inclusionService;

    @PostMapping("/{roomTypeId}/decrement")
    public ResponseEntity<Void> decrement(
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody RoomTypeAvailabilityRequest req) {
        roomTypeService.decrementAvailability(roomTypeId, req.fromDate(), req.toDate(), req.count());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomTypeId}/increment")
    public ResponseEntity<Void> increment(
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody RoomTypeAvailabilityRequest req) {
        roomTypeService.incrementAvailability(roomTypeId, req.fromDate(), req.toDate(), req.count());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomTypeId}/occupy")
    public ResponseEntity<Void> occupy(
            @PathVariable UUID roomTypeId,
            @RequestParam String sharingType) {
        roomTypeService.incrementOccupancy(roomTypeId, sharingType);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomTypeId}/release")
    public ResponseEntity<Void> release(
            @PathVariable UUID roomTypeId,
            @RequestParam String sharingType,
            @RequestParam(required = false) String moveOutDate) {
        java.time.LocalDate date = moveOutDate != null ? java.time.LocalDate.parse(moveOutDate) : null;
        roomTypeService.decrementOccupancy(roomTypeId, sharingType, date);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomTypeId}/occupy-booking")
    public ResponseEntity<Void> occupyBooking(
            @PathVariable UUID roomTypeId,
            @RequestParam int rooms) {
        roomTypeService.incrementOccupancyForBooking(roomTypeId, rooms);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomTypeId}/release-booking")
    public ResponseEntity<Void> releaseBooking(
            @PathVariable UUID roomTypeId,
            @RequestParam int rooms) {
        roomTypeService.decrementOccupancyForBooking(roomTypeId, rooms);
        return ResponseEntity.ok().build();
    }

    /**
     * Force-set occupied beds. Used to reconcile drift caused by cross-version
     * bookings (e.g. semantic change in how PRIVATE vs shared types count beds).
     * Clamped to [0, totalBeds] by the service layer.
     *
     * Example: curl -X POST "http://localhost:8083/api/v1/internal/room-types/{id}/set-occupancy?beds=0"
     */
    @PostMapping("/{roomTypeId}/set-occupancy")
    public ResponseEntity<Void> setOccupancy(
            @PathVariable UUID roomTypeId,
            @RequestParam int beds) {
        roomTypeService.setOccupiedBeds(roomTypeId, beds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomTypeId}")
    public ResponseEntity<RoomTypeResponse> getRoomType(@PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypeById(roomTypeId));
    }

    @GetMapping("/{roomTypeId}/inclusions")
    public ResponseEntity<List<RoomTypeInclusionResponse>> getInclusions(@PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(inclusionService.getInclusions(roomTypeId));
    }
}
