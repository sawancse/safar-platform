package com.safar.listing.controller;

import com.safar.listing.entity.LocalityPolygon;
import com.safar.listing.service.LocalityPolygonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/localities")
@RequiredArgsConstructor
public class LocalityPolygonController {

    private final LocalityPolygonService service;

    @PostMapping("/import")
    public ResponseEntity<LocalityPolygon> importFromOsm(
            @RequestParam String name,
            @RequestParam String city,
            @RequestParam(required = false) String state) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.fetchAndStoreFromOsm(name, city, state));
    }

    @PostMapping("/bulk-import")
    public ResponseEntity<List<LocalityPolygon>> bulkImport(@RequestBody List<Map<String, String>> localities) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bulkImport(localities));
    }

    @GetMapping
    public ResponseEntity<List<LocalityPolygon>> listAll(@RequestParam(required = false) String city) {
        if (city != null) {
            return ResponseEntity.ok(service.listByCity(city));
        }
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocalityPolygon> getPolygon(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPolygon(id));
    }

    @GetMapping("/by-name")
    public ResponseEntity<LocalityPolygon> getByNameAndCity(
            @RequestParam String name, @RequestParam String city) {
        return service.getPolygonByNameAndCity(name, city)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<LocalityPolygon>> listByCity(@PathVariable String city) {
        return ResponseEntity.ok(service.listByCity(city));
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<LocalityPolygon> refreshFromOsm(@PathVariable UUID id) {
        return ResponseEntity.ok(service.refreshFromOsm(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(service.getStatsByCity());
    }
}
