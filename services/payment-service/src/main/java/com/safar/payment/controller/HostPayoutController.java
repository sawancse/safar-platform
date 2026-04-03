package com.safar.payment.controller;

import com.safar.payment.entity.HostPayout;
import com.safar.payment.service.HostPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/host-payouts")
@RequiredArgsConstructor
public class HostPayoutController {

    private final HostPayoutService hostPayoutService;

    @GetMapping
    public ResponseEntity<Page<HostPayout>> getPayouts(
            @RequestParam UUID hostId,
            Pageable pageable) {
        return ResponseEntity.ok(hostPayoutService.getPayouts(hostId, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPayoutSummary(
            @RequestParam UUID hostId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(hostPayoutService.getPayoutSummary(hostId, month, year));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<HostPayout> executeTransfer(@PathVariable UUID id) {
        return ResponseEntity.ok(hostPayoutService.executeTransfer(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<HostPayout> retryPayout(@PathVariable UUID id) {
        return ResponseEntity.ok(hostPayoutService.retryPayout(id));
    }
}
