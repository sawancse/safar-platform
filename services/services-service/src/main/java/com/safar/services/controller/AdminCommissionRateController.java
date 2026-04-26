package com.safar.services.controller;

import com.safar.services.entity.CommissionRateConfigEntity;
import com.safar.services.service.CommissionRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for editing platform commission rates without redeploy.
 *
 * Per-(service_type, tier) rate edits surface within ~5 min via the
 * CommissionRateService cache; saveRate() forces an immediate refresh.
 *
 * Routes:
 *   GET  /api/v1/services/admin/commission-rates
 *   GET  /api/v1/services/admin/commission-rates?serviceType=CAKE_DESIGNER
 *   PUT  /api/v1/services/admin/commission-rates/{serviceType}/{tier}
 *        body: { commissionPct: 12.50, promotionThreshold: 8, notes: "..." }
 */
@RestController
@RequestMapping("/api/v1/services/admin/commission-rates")
@RequiredArgsConstructor
public class AdminCommissionRateController {

    private final CommissionRateService rateService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    @GetMapping
    public ResponseEntity<List<CommissionRateConfigEntity>> list(
            Authentication auth,
            @RequestParam(required = false) String serviceType) {
        requireAdmin(auth);
        return ResponseEntity.ok(serviceType != null
                ? rateService.listForType(serviceType)
                : rateService.listAll());
    }

    public record RateUpdate(BigDecimal commissionPct, Integer promotionThreshold, String notes) {}

    @PutMapping("/{serviceType}/{tier}")
    public ResponseEntity<CommissionRateConfigEntity> update(
            Authentication auth,
            @PathVariable String serviceType,
            @PathVariable String tier,
            @RequestBody RateUpdate body) {
        requireAdmin(auth);
        if (body.commissionPct() == null || body.commissionPct().signum() < 0
                || body.commissionPct().compareTo(new BigDecimal("50")) > 0) {
            throw new IllegalArgumentException("commissionPct must be in [0, 50]");
        }

        CommissionRateConfigEntity row = rateService.listForType(serviceType).stream()
                .filter(r -> r.getTier().equals(tier))
                .findFirst()
                .orElseGet(() -> CommissionRateConfigEntity.builder()
                        .serviceType(serviceType).tier(tier).build());

        row.setCommissionPct(body.commissionPct());
        if (body.promotionThreshold() != null) row.setPromotionThreshold(body.promotionThreshold());
        if (body.notes() != null) row.setNotes(body.notes());
        row.setUpdatedBy(UUID.fromString(auth.getName()));

        return ResponseEntity.ok(rateService.saveRate(row));
    }
}
