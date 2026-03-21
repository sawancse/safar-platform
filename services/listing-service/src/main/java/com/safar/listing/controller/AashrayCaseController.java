package com.safar.listing.controller;

import com.safar.listing.entity.AashrayCase;
import com.safar.listing.entity.enums.CaseStatus;
import com.safar.listing.service.AashrayCaseService;
import com.safar.listing.service.AashrayMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aashray/cases")
@RequiredArgsConstructor
public class AashrayCaseController {

    private final AashrayCaseService caseService;
    private final AashrayMatchingService matchingService;

    @PostMapping
    public ResponseEntity<AashrayCase> createCase(@RequestBody AashrayCase aashrayCase) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caseService.createCase(aashrayCase));
    }

    @GetMapping
    public ResponseEntity<Page<AashrayCase>> getCases(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(caseService.getCases(status, city, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AashrayCase> getCase(@PathVariable UUID id) {
        return ResponseEntity.ok(caseService.getCase(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AashrayCase> updateCase(@PathVariable UUID id, @RequestBody AashrayCase update) {
        return ResponseEntity.ok(caseService.updateCase(id, update));
    }

    @PostMapping("/{id}/match")
    public ResponseEntity<List<AashrayMatchingService.MatchResult>> findMatches(@PathVariable UUID id) {
        AashrayCase aashrayCase = caseService.getCase(id);
        return ResponseEntity.ok(matchingService.findMatches(aashrayCase));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<AashrayCase> assignToListing(
            @PathVariable UUID id, @RequestParam UUID listingId) {
        return ResponseEntity.ok(caseService.assignToListing(id, listingId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AashrayCase> updateStatus(
            @PathVariable UUID id, @RequestParam CaseStatus status) {
        return ResponseEntity.ok(caseService.updateStatus(id, status));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(caseService.getStats());
    }
}
