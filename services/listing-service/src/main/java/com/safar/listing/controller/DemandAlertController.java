package com.safar.listing.controller;

import com.safar.listing.dto.DemandAlertDto;
import com.safar.listing.service.DemandAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{id}/demand-alert")
@RequiredArgsConstructor
public class DemandAlertController {

    private final DemandAlertService demandAlertService;

    @GetMapping
    public ResponseEntity<DemandAlertDto> getDemandAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(demandAlertService.computeAlert(id));
    }
}
