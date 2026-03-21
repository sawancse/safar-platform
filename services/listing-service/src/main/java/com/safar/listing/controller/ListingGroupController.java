package com.safar.listing.controller;

import com.safar.listing.dto.CreateGroupRequest;
import com.safar.listing.dto.ListingGroupResponse;
import com.safar.listing.service.ListingGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listing-groups")
@RequiredArgsConstructor
public class ListingGroupController {

    private final ListingGroupService listingGroupService;

    @PostMapping
    public ResponseEntity<ListingGroupResponse> create(Authentication auth,
                                                        @Valid @RequestBody CreateGroupRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingGroupService.createGroup(hostId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingGroupResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(listingGroupService.getGroup(id));
    }

    @PutMapping("/{id}/members")
    public ResponseEntity<ListingGroupResponse> updateMembers(@PathVariable UUID id,
                                                               @RequestParam UUID listingId,
                                                               @RequestParam(defaultValue = "add") String action) {
        if ("remove".equalsIgnoreCase(action)) {
            return ResponseEntity.ok(listingGroupService.removeMember(id, listingId));
        }
        return ResponseEntity.ok(listingGroupService.addMember(id, listingId));
    }
}
