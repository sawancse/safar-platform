package com.safar.listing.controller;

import com.safar.listing.entity.ChannelManagerProperty;
import com.safar.listing.entity.ChannelMapping;
import com.safar.listing.entity.ChannelSyncLog;
import com.safar.listing.service.ChannelManagerService;
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
@RequestMapping("/api/v1/channel-manager")
@RequiredArgsConstructor
public class ChannelManagerController {

    private final ChannelManagerService service;

    @PostMapping("/connect/{listingId}")
    public ResponseEntity<ChannelManagerProperty> connect(@PathVariable UUID listingId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.connectProperty(listingId));
    }

    @DeleteMapping("/disconnect/{listingId}")
    public ResponseEntity<Void> disconnect(@PathVariable UUID listingId) {
        service.disconnectProperty(listingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync/rates/{listingId}")
    public ResponseEntity<Map<String, String>> syncRates(@PathVariable UUID listingId) {
        service.syncRates(listingId);
        return ResponseEntity.ok(Map.of("status", "Rates synced successfully"));
    }

    @PostMapping("/sync/availability/{listingId}")
    public ResponseEntity<Map<String, String>> syncAvailability(@PathVariable UUID listingId) {
        service.syncAvailability(listingId);
        return ResponseEntity.ok(Map.of("status", "Availability synced successfully"));
    }

    @PostMapping("/sync/pull-bookings/{listingId}")
    public ResponseEntity<Map<String, Object>> pullBookings(@PathVariable UUID listingId) {
        int count = service.pullBookings(listingId);
        return ResponseEntity.ok(Map.of("status", "Bookings pulled", "count", count));
    }

    @GetMapping("/status/{listingId}")
    public ResponseEntity<ChannelManagerProperty> getStatus(@PathVariable UUID listingId) {
        return ResponseEntity.ok(service.getConnectionStatus(listingId));
    }

    @GetMapping("/logs/{listingId}")
    public ResponseEntity<Page<ChannelSyncLog>> getLogs(@PathVariable UUID listingId, Pageable pageable) {
        return ResponseEntity.ok(service.getSyncLogs(listingId, pageable));
    }

    @GetMapping("/channels")
    public ResponseEntity<List<String>> getAvailableChannels() {
        return ResponseEntity.ok(List.of(
                "AIRBNB", "BOOKING_COM", "MMT", "OYO", "AGODA", "EXPEDIA", "GOIBIBO"
        ));
    }

    @GetMapping("/mappings/{listingId}")
    public ResponseEntity<List<ChannelMapping>> getMappings(@PathVariable UUID listingId) {
        return ResponseEntity.ok(service.getMappings(listingId));
    }

    @PostMapping("/mappings")
    public ResponseEntity<ChannelMapping> createMapping(@RequestBody ChannelMapping mapping) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createMapping(mapping));
    }

    @DeleteMapping("/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable UUID id) {
        service.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }
}
