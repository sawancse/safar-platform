package com.safar.payment.controller;

import com.safar.payment.dto.CommissionBreakdown;
import com.safar.payment.dto.FxLockRequest;
import com.safar.payment.dto.GstBreakdown;
import com.safar.payment.dto.RefundRequest;
import com.safar.payment.entity.*;
import com.safar.payment.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final EscrowService escrowService;
    private final RefundService refundService;
    private final LedgerService ledgerService;
    private final FxService fxService;
    private final CommissionService commissionService;
    private final GstInvoiceService gstInvoiceService;

    // --- Settlement ---

    @GetMapping("/settlements/{bookingId}")
    public ResponseEntity<SettlementPlan> getSettlement(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(settlementService.getSettlementPlan(bookingId));
    }

    // --- Escrow ---

    @GetMapping("/escrow/{bookingId}")
    public ResponseEntity<List<EscrowEntry>> getEscrowStatus(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(escrowService.getEscrowStatus(bookingId));
    }

    // --- Refunds ---

    @PostMapping("/refund")
    public ResponseEntity<RefundRecord> initiateRefund(@Valid @RequestBody RefundRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundService.initiateRefund(req));
    }

    @GetMapping("/refunds/{bookingId}")
    public ResponseEntity<List<RefundRecord>> getRefunds(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(refundService.getRefunds(bookingId));
    }

    // --- Ledger ---

    @GetMapping("/ledger/{bookingId}")
    public ResponseEntity<List<LedgerEntry>> getLedger(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ledgerService.getBookingLedger(bookingId));
    }

    // --- FX ---

    @GetMapping("/fx/rate")
    public ResponseEntity<Map<String, Object>> getFxRate(@RequestParam String from,
                                                          @RequestParam String to) {
        var rate = fxService.getRate(from, to);
        return ResponseEntity.ok(Map.of(
                "from", from,
                "to", to,
                "rate", rate
        ));
    }

    @PostMapping("/fx/lock")
    public ResponseEntity<FxLock> lockFxRate(@RequestParam String sourceCurrency,
                                              @RequestParam Long sourceAmount,
                                              @RequestParam(required = false) UUID bookingId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fxService.lockRate(sourceCurrency, sourceAmount, bookingId));
    }

    @GetMapping("/fx/currencies")
    public ResponseEntity<Set<String>> getSupportedCurrencies() {
        return ResponseEntity.ok(fxService.supportedCurrencies());
    }

    // --- Commission ---

    @GetMapping("/commission/calculate")
    public ResponseEntity<CommissionBreakdown> calculateCommission(
            @RequestParam long accommodationPaise,
            @RequestParam(defaultValue = "0") long treatmentPaise,
            @RequestParam(defaultValue = "STARTER") String tier,
            @RequestParam(defaultValue = "STANDARD") String bookingType) {
        return ResponseEntity.ok(commissionService.calculate(
                accommodationPaise, treatmentPaise, tier, bookingType));
    }

    // --- GST ---

    @GetMapping("/gst/calculate")
    public ResponseEntity<GstBreakdown> calculateGst(
            @RequestParam long perNightPaise,
            @RequestParam int nights,
            @RequestParam String guestState,
            @RequestParam String propertyState,
            @RequestParam(defaultValue = "STANDARD") String bookingType) {
        return ResponseEntity.ok(gstInvoiceService.calculateGst(
                perNightPaise, nights, guestState, propertyState, bookingType));
    }
}
