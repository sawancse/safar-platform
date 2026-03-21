package com.safar.listing.controller;

import com.safar.listing.entity.ICalFeed;
import com.safar.listing.service.ICalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/ical")
@RequiredArgsConstructor
public class ICalController {

    private final ICalService icalService;

    @GetMapping(value = "/export", produces = "text/calendar")
    public ResponseEntity<String> exportCalendar(@PathVariable UUID listingId) {
        String calendar = icalService.exportCalendar(listingId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header("Content-Disposition", "attachment; filename=\"safar-calendar.ics\"")
                .body(calendar);
    }

    @PostMapping("/feeds")
    public ResponseEntity<ICalFeed> addFeed(Authentication auth,
                                             @PathVariable UUID listingId,
                                             @RequestBody Map<String, String> body) {
        UUID hostId = UUID.fromString(auth.getName());
        String feedUrl = body.get("feedUrl");
        String feedName = body.get("feedName");
        ICalFeed feed = icalService.importFeed(listingId, hostId, feedUrl, feedName);
        return ResponseEntity.status(HttpStatus.CREATED).body(feed);
    }

    @GetMapping("/feeds")
    public ResponseEntity<List<ICalFeed>> getFeeds(@PathVariable UUID listingId) {
        return ResponseEntity.ok(icalService.getFeeds(listingId));
    }

    @PostMapping("/feeds/{feedId}/sync")
    public ResponseEntity<Map<String, String>> syncFeed(Authentication auth,
                                                         @PathVariable UUID listingId,
                                                         @PathVariable UUID feedId) {
        icalService.syncFeed(feedId);
        return ResponseEntity.ok(Map.of("status", "synced"));
    }

    @DeleteMapping("/feeds/{feedId}")
    public ResponseEntity<Void> deleteFeed(Authentication auth,
                                            @PathVariable UUID listingId,
                                            @PathVariable UUID feedId) {
        UUID hostId = UUID.fromString(auth.getName());
        icalService.deleteFeed(feedId, hostId);
        return ResponseEntity.noContent().build();
    }
}
