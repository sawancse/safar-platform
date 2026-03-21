package com.safar.listing.controller;

import com.safar.listing.dto.DemandTrendDto;
import com.safar.listing.dto.OccupancyRateDto;
import com.safar.listing.dto.RentalYieldDto;
import com.safar.listing.service.B2bApiKeyValidator;
import com.safar.listing.service.DataIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/b2b/v1")
@RequiredArgsConstructor
public class DataIntelligenceController {

    private final B2bApiKeyValidator apiKeyValidator;
    private final DataIntelligenceService dataIntelligenceService;

    @GetMapping("/rental-yields")
    public ResponseEntity<RentalYieldDto> getRentalYields(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestParam String city) {
        apiKeyValidator.validate(apiKey);
        return ResponseEntity.ok(dataIntelligenceService.getRentalYields(city));
    }

    @GetMapping("/demand-trends")
    public ResponseEntity<List<DemandTrendDto>> getDemandTrends(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestParam String city,
            @RequestParam(defaultValue = "6") int months) {
        apiKeyValidator.validate(apiKey);
        return ResponseEntity.ok(dataIntelligenceService.getDemandTrends(city, months));
    }

    @GetMapping("/occupancy-rates")
    public ResponseEntity<List<OccupancyRateDto>> getOccupancyRates(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestParam String city) {
        apiKeyValidator.validate(apiKey);
        return ResponseEntity.ok(dataIntelligenceService.getOccupancyRates(city));
    }
}
