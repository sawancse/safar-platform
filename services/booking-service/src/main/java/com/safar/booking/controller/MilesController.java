package com.safar.booking.controller;

import com.safar.booking.dto.RedeemMilesRequest;
import com.safar.booking.entity.MilesBalance;
import com.safar.booking.entity.MilesLedger;
import com.safar.booking.service.MilesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/miles")
@RequiredArgsConstructor
public class MilesController {

    private final MilesService milesService;

    @GetMapping("/balance")
    public ResponseEntity<MilesBalance> getBalance(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(milesService.getBalance(userId));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<MilesLedger>> getHistory(Authentication auth, Pageable pageable) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(milesService.getHistory(userId, pageable));
    }

    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Long>> redeem(Authentication auth,
                                                     @Valid @RequestBody RedeemMilesRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        long discountPaise = milesService.redeemMiles(userId, req.bookingId(), req.miles(),
                /* totalPaise fetched from booking context */ 0L);
        return ResponseEntity.ok(Map.of("discountPaise", discountPaise));
    }
}
