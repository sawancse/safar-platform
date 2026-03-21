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

    @GetMapping("/{roomTypeId}")
    public ResponseEntity<RoomTypeResponse> getRoomType(@PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypeById(roomTypeId));
    }

    @GetMapping("/{roomTypeId}/inclusions")
    public ResponseEntity<List<RoomTypeInclusionResponse>> getInclusions(@PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(inclusionService.getInclusions(roomTypeId));
    }
}
