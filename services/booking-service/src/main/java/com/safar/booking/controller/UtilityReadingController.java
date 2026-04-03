package com.safar.booking.controller;

import com.safar.booking.dto.RecordUtilityReadingRequest;
import com.safar.booking.dto.UtilityReadingResponse;
import com.safar.booking.entity.UtilityReading;
import com.safar.booking.service.UtilityReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pg-tenancies/{tenancyId}/utility-readings")
@RequiredArgsConstructor
public class UtilityReadingController {

    private final UtilityReadingService utilityReadingService;

    @PostMapping
    public ResponseEntity<UtilityReadingResponse> recordReading(
            @PathVariable UUID tenancyId,
            @RequestBody RecordUtilityReadingRequest request) {
        UtilityReading reading = utilityReadingService.recordReading(tenancyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(utilityReadingService.toResponse(reading));
    }

    @GetMapping
    public ResponseEntity<List<UtilityReadingResponse>> getReadings(
            @PathVariable UUID tenancyId,
            @RequestParam(required = false) String utilityType) {
        List<UtilityReadingResponse> readings = utilityReadingService.getReadings(tenancyId, utilityType)
                .stream()
                .map(utilityReadingService::toResponse)
                .toList();
        return ResponseEntity.ok(readings);
    }

    @GetMapping("/unbilled")
    public ResponseEntity<Map<String, Long>> getUnbilledCharges(@PathVariable UUID tenancyId) {
        long electricity = utilityReadingService.getUnbilledElectricity(tenancyId);
        long water = utilityReadingService.getUnbilledWater(tenancyId);
        return ResponseEntity.ok(Map.of(
                "electricityPaise", electricity,
                "waterPaise", water,
                "totalPaise", electricity + water
        ));
    }
}
