package com.safar.listing.controller;

import com.safar.listing.dto.ExperienceBookingRequest;
import com.safar.listing.dto.ExperienceRequest;
import com.safar.listing.dto.ExperienceResponse;
import com.safar.listing.dto.SessionRequest;
import com.safar.listing.entity.ExperienceBooking;
import com.safar.listing.entity.ExperienceSession;
import com.safar.listing.entity.enums.ExperienceStatus;
import com.safar.listing.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @PostMapping("/api/v1/experiences")
    public ResponseEntity<ExperienceResponse> create(Authentication auth,
                                              @Valid @RequestBody ExperienceRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(experienceService.createExperience(hostId, req));
    }

    @PutMapping("/api/v1/experiences/{id}")
    public ResponseEntity<ExperienceResponse> update(Authentication auth,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody ExperienceRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(experienceService.updateExperience(hostId, id, req));
    }

    @PatchMapping("/api/v1/experiences/{id}/status")
    public ResponseEntity<ExperienceResponse> updateStatus(Authentication auth,
                                                            @PathVariable UUID id,
                                                            @RequestBody Map<String, String> body) {
        UUID hostId = UUID.fromString(auth.getName());
        ExperienceStatus newStatus = ExperienceStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(experienceService.updateStatus(hostId, id, newStatus));
    }

    @GetMapping("/api/v1/experiences")
    public ResponseEntity<Page<ExperienceResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(experienceService.searchExperiences(city, category, pageable));
    }

    @GetMapping("/api/v1/experiences/{id}")
    public ResponseEntity<ExperienceResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(experienceService.getExperience(id));
    }

    @GetMapping("/api/v1/experiences/host")
    public ResponseEntity<List<ExperienceResponse>> hostExperiences(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(experienceService.getHostExperiences(hostId));
    }

    @PostMapping("/api/v1/experiences/{id}/sessions")
    public ResponseEntity<ExperienceSession> addSession(Authentication auth,
                                                         @PathVariable UUID id,
                                                         @Valid @RequestBody SessionRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(experienceService.addSession(hostId, id, req));
    }

    @PostMapping("/api/v1/experience-bookings")
    public ResponseEntity<ExperienceBooking> book(Authentication auth,
                                                   @Valid @RequestBody ExperienceBookingRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(experienceService.bookExperience(guestId, req));
    }

    @GetMapping("/api/v1/experience-bookings")
    public ResponseEntity<List<ExperienceBooking>> myBookings(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(experienceService.getMyBookings(guestId));
    }
}
