package com.safar.listing.controller;

import com.safar.listing.dto.LocalityPriceTrendResponse;
import com.safar.listing.service.LocalityPriceTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locality-trends")
@RequiredArgsConstructor
public class LocalityPriceTrendController {

    private final LocalityPriceTrendService trendService;

    @GetMapping
    public ResponseEntity<List<LocalityPriceTrendResponse>> getTrends(
            @RequestParam String city,
            @RequestParam String locality) {
        return ResponseEntity.ok(trendService.getTrends(city, locality));
    }

    @GetMapping("/city")
    public ResponseEntity<List<LocalityPriceTrendResponse>> getCityLocalities(
            @RequestParam String city) {
        return ResponseEntity.ok(trendService.getCityLocalities(city));
    }
}
