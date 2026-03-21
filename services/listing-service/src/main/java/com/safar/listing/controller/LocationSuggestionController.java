package com.safar.listing.controller;

import com.safar.listing.service.LocationSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationSuggestionController {

    private final LocationSuggestionService locationService;

    @GetMapping("/suggest")
    public ResponseEntity<Map<String, List<LocationSuggestionService.LocationSuggestionDto>>> suggest(
            @RequestParam String q) {
        return ResponseEntity.ok(locationService.suggest(q));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<LocationSuggestionService.LocationSuggestionDto>> getByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(locationService.getByCity(city));
    }
}
