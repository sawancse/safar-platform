package com.safar.user.controller;

import com.safar.user.entity.NomadPrimeMembership;
import com.safar.user.service.NomadPrimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nomad-prime")
@RequiredArgsConstructor
public class NomadPrimeController {

    private final NomadPrimeService service;

    @PostMapping("/subscribe")
    public ResponseEntity<NomadPrimeMembership> subscribe(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.subscribe(guestId));
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<NomadPrimeMembership> cancel(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.cancel(guestId));
    }

    @GetMapping("/membership")
    public ResponseEntity<NomadPrimeMembership> getMembership(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.getMembership(guestId));
    }

    @GetMapping("/benefits")
    public ResponseEntity<Map<String, Object>> getBenefits() {
        return ResponseEntity.ok(Map.of(
                "name", "NomadPrime",
                "pricePaise", NomadPrimeService.NOMAD_PRIME_PRICE_PAISE,
                "discountPct", 15,
                "monthlyBonusMiles", 500,
                "insuranceCoverPaise", 50000000L,
                "description", "NomadPrime membership at Rs.999/month with 15% booking discount, 500 bonus miles/month, and Rs.5L travel insurance"
        ));
    }
}
