package com.safar.listing.controller;

import com.safar.listing.dto.PricingRuleRequest;
import com.safar.listing.dto.PricingRuleResponse;
import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.service.PricingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/pricing-rules")
@RequiredArgsConstructor
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;
    private final ListingRepository listingRepository;

    @PostMapping
    public ResponseEntity<PricingRuleResponse> createRule(Authentication auth,
                                                           @PathVariable UUID listingId,
                                                           @Valid @RequestBody PricingRuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pricingRuleService.createRule(listingId, req));
    }

    @GetMapping
    public ResponseEntity<List<PricingRuleResponse>> getRules(@PathVariable UUID listingId) {
        return ResponseEntity.ok(pricingRuleService.getRules(listingId));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<PricingRuleResponse> updateRule(Authentication auth,
                                                           @PathVariable UUID listingId,
                                                           @PathVariable UUID ruleId,
                                                           @Valid @RequestBody PricingRuleRequest req) {
        return ResponseEntity.ok(pricingRuleService.updateRule(listingId, ruleId, req));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(Authentication auth,
                                            @PathVariable UUID listingId,
                                            @PathVariable UUID ruleId) {
        pricingRuleService.deleteRule(listingId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewPrice(
            @PathVariable UUID listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID roomTypeId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        Long basePricePaise = listing.getBasePricePaise();
        Long effectivePrice = pricingRuleService.calculateEffectivePrice(listingId, roomTypeId, date, basePricePaise);

        return ResponseEntity.ok(Map.of(
                "date", date.toString(),
                "basePricePaise", basePricePaise,
                "effectivePricePaise", effectivePrice,
                "listingId", listingId.toString()
        ));
    }
}
