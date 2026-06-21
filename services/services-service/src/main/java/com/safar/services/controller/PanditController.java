package com.safar.services.controller;

import com.safar.services.dto.MatchPanditsRequest;
import com.safar.services.dto.MatchedPanditResponse;
import com.safar.services.service.PanditMatchingService;
import com.safar.services.service.PanditMuhuratService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Public pandit-vertical endpoints: rank verified pandits for a puja and serve
 * the Shubh Muhurat 2026 reference. Both are read-only and carry no PII.
 */
@RestController
@RequestMapping("/api/v1/services/pandit")
@RequiredArgsConstructor
public class PanditController {

    private final PanditMatchingService matchingService;
    private final PanditMuhuratService muhuratService;

    /** Ranked shortlist of verified pandits for the supplied criteria. */
    @PostMapping("/match")
    public ResponseEntity<List<MatchedPanditResponse>> match(@RequestBody MatchPanditsRequest req) {
        return ResponseEntity.ok(matchingService.match(req));
    }

    /**
     * Shubh Muhurat 2026 by category. Optional {@code from} (defaults today) trims
     * to upcoming dates; optional {@code occasion} filters to one category.
     */
    @GetMapping("/muhurat")
    public ResponseEntity<List<PanditMuhuratService.MuhuratEntry>> muhurat(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) String occasion) {
        return ResponseEntity.ok(muhuratService.upcoming(from != null ? from : LocalDate.now(), occasion));
    }
}
