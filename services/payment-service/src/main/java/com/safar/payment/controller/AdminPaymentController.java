package com.safar.payment.controller;

import com.safar.payment.entity.GstInvoice;
import com.safar.payment.entity.Payout;
import com.safar.payment.entity.SettlementPlan;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.repository.GstInvoiceRepository;
import com.safar.payment.repository.PayoutRepository;
import com.safar.payment.repository.SettlementPlanRepository;
import com.safar.payment.service.LedgerService;
import com.safar.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentController {

    private final PayoutRepository payoutRepository;
    private final SettlementPlanRepository settlementPlanRepository;
    private final LedgerService ledgerService;
    private final GstInvoiceRepository gstInvoiceRepository;
    private final SettlementService settlementService;

    @GetMapping("/payouts")
    public ResponseEntity<List<Payout>> getAllPayouts(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(payoutRepository.findAll());
    }

    @GetMapping("/payouts/status/{status}")
    public ResponseEntity<List<Payout>> getPayoutsByStatus(@PathVariable String status, Authentication auth) {
        requireAdmin(auth);
        try {
            PayoutStatus ps = PayoutStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(payoutRepository.findByStatus(ps));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(payoutRepository.findAll());
        }
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementPlan>> getAllSettlements(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(settlementPlanRepository.findAll());
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<Map<String, Object>> getReconciliation(Authentication auth) {
        requireAdmin(auth);
        var result = ledgerService.reconcile(LocalDate.now());
        return ResponseEntity.ok(Map.of(
                "date", LocalDate.now().toString(),
                "totalDebits", result.totalDebits(),
                "totalCredits", result.totalCredits(),
                "balanced", result.balanced(),
                "mismatches", result.mismatches()
        ));
    }

    @GetMapping("/commission/summary")
    public ResponseEntity<Map<String, Object>> getCommissionSummary(Authentication auth) {
        requireAdmin(auth);

        List<GstInvoice> allInvoices = gstInvoiceRepository.findAll();

        long totalTaxableAmount = allInvoices.stream().mapToLong(GstInvoice::getTaxableAmount).sum();

        List<Payout> allPayouts = payoutRepository.findAll();
        long totalPayoutAmount = allPayouts.stream()
                .filter(p -> p.getStatus() == PayoutStatus.COMPLETED)
                .mapToLong(Payout::getNetAmountPaise).sum();

        long totalCommission = totalTaxableAmount - totalPayoutAmount;
        if (totalCommission < 0) totalCommission = 0;

        double avgRate = totalTaxableAmount > 0 ? (double) totalCommission / totalTaxableAmount * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCommissionPaise", totalCommission);
        result.put("totalRevenuePaise", totalTaxableAmount);
        result.put("totalPayoutPaise", totalPayoutAmount);
        result.put("avgCommissionRate", Math.round(avgRate * 10) / 10.0);
        result.put("totalInvoices", allInvoices.size());
        result.put("totalPayouts", allPayouts.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/commission/by-host")
    public ResponseEntity<List<Map<String, Object>>> getCommissionByHost(Authentication auth) {
        requireAdmin(auth);

        List<GstInvoice> allInvoices = gstInvoiceRepository.findAll();

        Map<UUID, List<GstInvoice>> byHost = allInvoices.stream()
                .collect(Collectors.groupingBy(GstInvoice::getHostId));

        List<Map<String, Object>> results = new ArrayList<>();
        for (var entry : byHost.entrySet()) {
            UUID hostId = entry.getKey();
            List<GstInvoice> invoices = entry.getValue();
            long revenue = invoices.stream().mapToLong(GstInvoice::getTaxableAmount).sum();
            long gst = invoices.stream().mapToLong(i -> i.getCgstAmount() + i.getSgstAmount() + i.getIgstAmount()).sum();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hostId", hostId);
            row.put("bookings", invoices.size());
            row.put("grossRevenuePaise", revenue);
            row.put("gstCollectedPaise", gst);
            row.put("totalWithGstPaise", revenue + gst);
            results.add(row);
        }

        results.sort((a, b) -> Long.compare((long) b.get("grossRevenuePaise"), (long) a.get("grossRevenuePaise")));

        return ResponseEntity.ok(results);
    }

    // ── Manual settlement processing ────────────────────────────────────────

    @PostMapping("/settlements/{planId}/process")
    public ResponseEntity<SettlementPlan> processSettlement(@PathVariable UUID planId, Authentication auth) {
        requireAdmin(auth);
        log.info("Admin manually processing settlement plan {}", planId);
        SettlementPlan processed = settlementService.processSettlement(planId);
        return ResponseEntity.ok(processed);
    }

    @PostMapping("/settlements/by-booking/{bookingId}/process")
    public ResponseEntity<SettlementPlan> processSettlementByBooking(@PathVariable UUID bookingId, Authentication auth) {
        requireAdmin(auth);
        SettlementPlan plan = settlementPlanRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No settlement plan found for booking: " + bookingId));
        log.info("Admin manually processing settlement for booking {} (plan {})", bookingId, plan.getId());
        SettlementPlan processed = settlementService.processSettlement(plan.getId());
        return ResponseEntity.ok(processed);
    }

    // ── Retry failed payout ──────────────────────────────────────────────────

    @PostMapping("/payouts/{payoutId}/retry")
    public ResponseEntity<Payout> retryPayout(@PathVariable UUID payoutId, Authentication auth) {
        requireAdmin(auth);
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        if (payout.getStatus() != PayoutStatus.FAILED) {
            throw new IllegalStateException("Only FAILED payouts can be retried, current status: " + payout.getStatus());
        }
        log.info("Admin retrying failed payout {} for host {}", payoutId, payout.getHostId());
        payout.setStatus(PayoutStatus.PENDING);
        payout.setFailureReason(null);
        Payout saved = payoutRepository.save(payout);
        return ResponseEntity.ok(saved);
    }

    // ── Payouts by host ──────────────────────────────────────────────────────

    @GetMapping("/payouts/by-host/{hostId}")
    public ResponseEntity<List<Payout>> getPayoutsByHost(@PathVariable UUID hostId, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(payoutRepository.findByHostId(hostId));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new SecurityException("Admin access required");
        }
    }
}
