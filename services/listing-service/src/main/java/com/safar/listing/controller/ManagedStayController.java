package com.safar.listing.controller;

import com.safar.listing.dto.ManagedEnrollRequest;
import com.safar.listing.entity.ManagedStayContract;
import com.safar.listing.service.ManagedStayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ManagedStayController {

    private final ManagedStayService managedStayService;

    @PostMapping("/api/v1/listings/{id}/managed-stay/enroll")
    public ResponseEntity<ManagedStayContract> enroll(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody ManagedEnrollRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managedStayService.enrollListing(hostId, id, req));
    }

    @DeleteMapping("/api/v1/listings/{id}/managed-stay")
    public ResponseEntity<ManagedStayContract> terminate(
            Authentication auth,
            @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(managedStayService.terminateContract(hostId, id));
    }

    @GetMapping("/api/v1/managed-stay/contracts")
    public ResponseEntity<List<ManagedStayContract>> getContracts(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(managedStayService.getContracts(hostId));
    }
}
