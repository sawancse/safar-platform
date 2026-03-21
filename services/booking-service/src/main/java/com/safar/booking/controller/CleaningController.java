package com.safar.booking.controller;

import com.safar.booking.dto.CreateCleaningJobRequest;
import com.safar.booking.dto.RegisterCleanerRequest;
import com.safar.booking.entity.CleanerProfile;
import com.safar.booking.entity.CleaningJob;
import com.safar.booking.service.CleaningNetworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cleaning")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningNetworkService cleaningService;

    @PostMapping("/cleaner-profiles")
    public ResponseEntity<CleanerProfile> registerCleaner(Authentication auth,
                                                            @Valid @RequestBody RegisterCleanerRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(cleaningService.registerCleaner(userId, req));
    }

    @PostMapping("/jobs")
    public ResponseEntity<CleaningJob> createJob(Authentication auth,
                                                   @Valid @RequestBody CreateCleaningJobRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cleaningService.createJob(req));
    }

    @PutMapping("/jobs/{id}/complete")
    public ResponseEntity<CleaningJob> completeJob(Authentication auth,
                                                     @PathVariable UUID id) {
        UUID cleanerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(cleaningService.completeJob(cleanerId, id));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<CleaningJob>> getJobs(@RequestParam UUID listingId) {
        return ResponseEntity.ok(cleaningService.getJobsByListing(listingId));
    }
}
