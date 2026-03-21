package com.safar.listing.controller;

import com.safar.listing.dto.RoomTypeInclusionRequest;
import com.safar.listing.dto.RoomTypeInclusionResponse;
import com.safar.listing.service.RoomTypeInclusionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/room-types/{roomTypeId}/inclusions")
@RequiredArgsConstructor
public class RoomTypeInclusionController {

    private final RoomTypeInclusionService inclusionService;

    @PostMapping
    public ResponseEntity<RoomTypeInclusionResponse> create(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody RoomTypeInclusionRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inclusionService.create(listingId, roomTypeId, hostId, req));
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeInclusionResponse>> list(
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(inclusionService.getInclusions(roomTypeId));
    }

    @PutMapping("/{inclusionId}")
    public ResponseEntity<RoomTypeInclusionResponse> update(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @PathVariable UUID inclusionId,
            @Valid @RequestBody RoomTypeInclusionRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(inclusionService.update(listingId, roomTypeId, inclusionId, hostId, req));
    }

    @DeleteMapping("/{inclusionId}")
    public ResponseEntity<Void> delete(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @PathVariable UUID inclusionId) {
        UUID hostId = UUID.fromString(auth.getName());
        inclusionService.delete(listingId, roomTypeId, inclusionId, hostId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/bulk")
    public ResponseEntity<List<RoomTypeInclusionResponse>> bulkReplace(
            Authentication auth,
            @PathVariable UUID listingId,
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody List<RoomTypeInclusionRequest> requests) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(inclusionService.bulkCreate(listingId, roomTypeId, hostId, requests));
    }
}
