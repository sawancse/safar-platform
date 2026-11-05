package com.safar.payment.controller;

import com.safar.payment.entity.GstInvoice;
import com.safar.payment.entity.Payment;
import com.safar.payment.entity.Payout;
import com.safar.payment.entity.SettlementPlan;
import com.safar.payment.entity.enums.PaymentStatus;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.repository.GstInvoiceRepository;
import com.safar.payment.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;
    private final com.safar.payment.service.PayoutService payoutService;

    @GetMapping("/payouts")
    public ResponseEntity<List<Payout>> getAllPayouts(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(payoutRepository.findAll());
    }

    /** Admin visibility: payouts for one booking. */
    @GetMapping(value = "/payouts", params = "bookingId")
    public ResponseEntity<List<Payout>> getPayoutsByBooking(@RequestParam UUID bookingId, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(payoutRepository.findByBookingId(bookingId));
    }

    /** "Pay host": create-if-missing + send. Idempotent per settlement line. */
    @PostMapping("/payouts/by-settlement/{lineId}/execute")
    public ResponseEntity<?> executePayout(@PathVariable UUID lineId,
                                           @RequestParam(defaultValue = "false") boolean force, Authentication auth) {
        requireAdmin(auth);
        try {
            return ResponseEntity.ok(payoutService.executeForSettlementLine(lineId, force));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Preview: unpaid HOST lines already past checkout + dispute window, grouped by host. */
    @GetMapping("/payouts/eligible")
    public ResponseEntity<?> eligiblePayouts(Authentication auth) {
        requireAdmin(auth);
        var out = new ArrayList<Map<String, Object>>();
        payoutService.eligibleLinesByHost().forEach((hostId, lines) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hostId", hostId);
            row.put("lines", lines.size());
            row.put("totalPaise", lines.stream().mapToLong(l -> l.getAmountPaise()).sum());
            row.put("lineIds", lines.stream().map(l -> l.getId()).toList());
            out.add(row);
        });
        return ResponseEntity.ok(out);
    }

    /** Pays all eligible lines of one host as a single payout. force=true ignores the dispute window. */
    @PostMapping("/payouts/batch/{hostId}")
    public ResponseEntity<?> batchPayout(@PathVariable UUID hostId,
                                         @RequestParam(defaultValue = "false") boolean force, Authentication auth) {
        requireAdmin(auth);
        try {
            return payoutService.executeBatchForHost(hostId, force)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.ok(Map.of("message", "Nothing payable for this host yet")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/payouts/{id}/retry")
    public ResponseEntity<?> retryPayout(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        try {
            return ResponseEntity.ok(payoutService.retry(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    public ResponseEntity<SettlementPlan> processSettlementByBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) Long totalAmountPaise,
            @RequestParam(required = false) String bookingType,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) String hostTier,
            @RequestParam(required = false) Long hostPayoutPaise,
            @RequestParam(required = false) Long platformFeePaise,
            @RequestParam(required = false) Long gstPaise,
            @RequestParam(required = false) java.math.BigDecimal commissionRate,
            Authentication auth) {
        requireAdmin(auth);

        // Try existing plan first
        SettlementPlan plan = settlementPlanRepository.findByBookingId(bookingId).orElse(null);

        // No plan yet (nothing creates one at capture time) → create it now
        if (plan == null) {
            Payment captured = paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                    .filter(p -> p.getStatus() == PaymentStatus.CAPTURED)
                    .orElse(null);
            UUID paymentId = captured != null ? captured.getId() : null;
            long total = totalAmountPaise != null && totalAmountPaise > 0 ? totalAmountPaise
                    : captured != null ? captured.getAmountPaise() : 0L;

            if (total <= 0 || hostId == null) {
                throw new IllegalArgumentException("No settlement plan exists for booking " + bookingId
                        + ". Provide totalAmountPaise and hostId (plus hostPayoutPaise/platformFeePaise/gstPaise) to create one.");
            }

            if (hostPayoutPaise != null && platformFeePaise != null) {
                log.info("Creating settlement plan for booking {} from booking breakdown (total={} paise)", bookingId, total);
                plan = settlementService.createSettlementPlanFromBooking(
                        bookingId, paymentId, total, hostId, hostPayoutPaise, platformFeePaise,
                        gstPaise != null ? gstPaise : 0L, commissionRate);
            } else {
                log.info("Creating settlement plan for booking {} from host tier (total={} paise)", bookingId, total);
                plan = settlementService.createSettlementPlan(
                        bookingId, paymentId, total,
                        bookingType != null ? bookingType : "SHORT_TERM",
                        hostId, null, hostTier != null ? hostTier : "STARTER");
            }
        }

        log.info("Admin processing settlement for booking {} (plan {})", bookingId, plan.getId());
        SettlementPlan processed = settlementService.processSettlement(plan.getId());
        return ResponseEntity.ok(processed);
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
