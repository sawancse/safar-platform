package com.safar.payment.controller;

import com.safar.payment.dto.*;
import com.safar.payment.entity.GstInvoice;
import com.safar.payment.entity.HostExpense;
import com.safar.payment.entity.HostInvoice;
import com.safar.payment.entity.HostTaxProfile;
import com.safar.payment.entity.Payout;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.repository.GstInvoiceRepository;
import com.safar.payment.repository.HostInvoiceRepository;
import com.safar.payment.repository.PayoutRepository;
import com.safar.payment.service.GstInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/host")
@RequiredArgsConstructor
public class HostBusinessController {

    private final GstInvoiceService gstInvoiceService;
    private final PayoutRepository payoutRepository;
    private final GstInvoiceRepository gstInvoiceRepository;
    private final HostInvoiceRepository hostInvoiceRepository;

    @PostMapping("/tax-profile")
    public ResponseEntity<HostTaxProfile> createTaxProfile(Authentication auth,
                                                            @Valid @RequestBody TaxProfileRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gstInvoiceService.createTaxProfile(hostId, req));
    }

    @GetMapping("/tax-profile")
    public ResponseEntity<HostTaxProfile> getTaxProfile(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(gstInvoiceService.getTaxProfile(hostId));
    }

    @PostMapping("/expenses")
    public ResponseEntity<HostExpense> logExpense(Authentication auth,
                                                   @Valid @RequestBody ExpenseRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gstInvoiceService.logExpense(hostId, req));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<HostExpense>> getExpenses(Authentication auth,
                                                          @RequestParam int year) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(gstInvoiceService.getExpenses(hostId, year));
    }

    @GetMapping("/gst-invoices")
    public ResponseEntity<List<GstInvoice>> getGstInvoices(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(gstInvoiceService.getGstInvoices(hostId));
    }

    @GetMapping("/tds-report")
    public ResponseEntity<TdsReport> getTdsReport(Authentication auth,
                                                    @RequestParam int year,
                                                    @RequestParam int quarter) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(gstInvoiceService.generateTdsReport(hostId, year, quarter));
    }

    @GetMapping("/pnl")
    public ResponseEntity<PnlStatement> getPnl(Authentication auth,
                                                 @RequestParam int year) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(gstInvoiceService.generatePnl(hostId, year));
    }

    // ── Payouts ──────────────────────────────────────────────────────────────

    @GetMapping("/payouts")
    public ResponseEntity<List<Payout>> getMyPayouts(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(payoutRepository.findByHostId(hostId));
    }

    @GetMapping("/payouts/summary")
    public ResponseEntity<Map<String, Object>> getPayoutSummary(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        List<Payout> payouts = payoutRepository.findByHostId(hostId);

        long totalPaidPaise = payouts.stream()
                .filter(p -> p.getStatus() == PayoutStatus.COMPLETED)
                .mapToLong(Payout::getNetAmountPaise)
                .sum();
        long pendingPaise = payouts.stream()
                .filter(p -> p.getStatus() == PayoutStatus.PENDING)
                .mapToLong(Payout::getNetAmountPaise)
                .sum();
        long failedCount = payouts.stream()
                .filter(p -> p.getStatus() == PayoutStatus.FAILED)
                .count();

        return ResponseEntity.ok(Map.of(
                "totalPaidPaise", totalPaidPaise,
                "pendingPaise", pendingPaise,
                "totalPayouts", payouts.size(),
                "failedCount", failedCount
        ));
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> getMyTransactions(
            Authentication auth,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID hostId = UUID.fromString(auth.getName());

        List<Map<String, Object>> transactions = new ArrayList<>();

        // GST Invoices as booking transactions
        List<GstInvoice> invoices = gstInvoiceRepository.findByHostId(hostId);
        for (GstInvoice inv : invoices) {
            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("id", inv.getId());
            tx.put("date", inv.getInvoiceDate());
            tx.put("type", "BOOKING");
            tx.put("description", "Booking \u2014 " + inv.getInvoiceNumber());
            tx.put("amountPaise", inv.getTotalAmount());
            tx.put("isCredit", true);
            transactions.add(tx);

            // TDS deduction
            long tds = Math.round(inv.getTaxableAmount() * 0.01);
            if (tds > 0) {
                Map<String, Object> tdsTx = new LinkedHashMap<>();
                tdsTx.put("id", "tds-" + inv.getId());
                tdsTx.put("date", inv.getInvoiceDate());
                tdsTx.put("type", "TDS");
                tdsTx.put("description", "TDS (1%) \u2014 " + inv.getInvoiceNumber());
                tdsTx.put("amountPaise", tds);
                tdsTx.put("isCredit", false);
                transactions.add(tdsTx);
            }
        }

        // Payouts
        List<Payout> payouts = payoutRepository.findByHostId(hostId);
        for (Payout p : payouts) {
            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("id", p.getId());
            tx.put("date", p.getCreatedAt());
            tx.put("type", "PAYOUT");
            tx.put("description", "Payout \u2014 " + p.getMethod() + " (" + p.getStatus() + ")");
            tx.put("amountPaise", p.getNetAmountPaise());
            tx.put("isCredit", true);
            transactions.add(tx);
        }

        // Subscription invoices
        List<HostInvoice> subInvoices = hostInvoiceRepository.findByHostId(hostId);
        for (HostInvoice inv : subInvoices) {
            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("id", inv.getId());
            tx.put("date", inv.getCreatedAt());
            tx.put("type", "SUBSCRIPTION");
            tx.put("description", inv.getTier() + " plan \u2014 " + inv.getInvoiceNumber());
            tx.put("amountPaise", inv.getTotalPaise());
            tx.put("isCredit", false);
            transactions.add(tx);
        }

        // Filter by type
        if (type != null && !type.isBlank()) {
            transactions.removeIf(tx -> !type.equals(tx.get("type")));
        }

        // Filter by date range
        if (from != null) {
            transactions.removeIf(tx -> {
                Object d = tx.get("date");
                if (d == null) return true;
                String dateStr = d.toString().substring(0, 10);
                return dateStr.compareTo(from.toString()) < 0;
            });
        }
        if (to != null) {
            transactions.removeIf(tx -> {
                Object d = tx.get("date");
                if (d == null) return true;
                String dateStr = d.toString().substring(0, 10);
                return dateStr.compareTo(to.toString()) > 0;
            });
        }

        // Sort by date descending
        transactions.sort((a, b) -> {
            String da = String.valueOf(a.get("date"));
            String db = String.valueOf(b.get("date"));
            return db.compareTo(da);
        });

        return ResponseEntity.ok(transactions);
    }
}
