package com.safar.user.controller;

import com.safar.user.dto.BucketListItemDto;
import com.safar.user.service.BucketListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bucket-list")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;

    @PostMapping("/{listingId}")
    public ResponseEntity<BucketListItemDto> add(Authentication auth,
                                                  @PathVariable UUID listingId,
                                                  @RequestParam(required = false) String notes) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bucketListService.add(guestId, listingId, notes));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> remove(Authentication auth,
                                        @PathVariable UUID listingId) {
        UUID guestId = UUID.fromString(auth.getName());
        bucketListService.remove(guestId, listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<BucketListItemDto>> list(Authentication auth,
                                                         Pageable pageable) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bucketListService.list(guestId, pageable));
    }
}
