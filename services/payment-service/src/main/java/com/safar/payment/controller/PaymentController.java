package com.safar.payment.controller;

import com.safar.payment.dto.InitiatePayoutRequest;
import com.safar.payment.dto.PaymentOrderResponse;
import com.safar.payment.dto.VerifyPaymentRequest;
import com.safar.payment.entity.HostInvoice;
import com.safar.payment.entity.Payout;
import com.safar.payment.service.BookingPaymentService;
import com.safar.payment.service.CommissionService;
import com.safar.payment.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final SubscriptionService subscriptionService;
    private final BookingPaymentService bookingPaymentService;
    private final CommissionService commissionService;

    @PostMapping("/subscription/create")
    public ResponseEntity<HostInvoice> createSubscription(Authentication auth,
                                                           @RequestParam String tier) throws Exception {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(hostId, tier));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<HostInvoice>> getInvoices(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(subscriptionService.getInvoices(hostId));
    }

    @PostMapping(value = "/webhook/razorpay", consumes = {"application/json", "text/plain"})
    public ResponseEntity<Void> webhookRazorpay(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        // Route to subscription or booking payment handler based on event
        try {
            subscriptionService.handleWebhook(payload, signature);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception ignored) {
            // Not a subscription event; try payment webhook
        }
        bookingPaymentService.handlePaymentWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @RequestParam UUID bookingId,
            @RequestParam(required = false) Long amountPaise) throws Exception {
        return ResponseEntity.ok(bookingPaymentService.createOrder(bookingId, amountPaise));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(@RequestBody VerifyPaymentRequest req) {
        bookingPaymentService.verifyPayment(
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payout")
    public ResponseEntity<Payout> initiatePayout(Authentication auth,
                                                  @Valid @RequestBody InitiatePayoutRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingPaymentService.initiateHostPayout(
                        hostId, req.bookingId(), req.amountPaise(), req.upiId()));
    }

    @GetMapping("/commission-rate")
    public ResponseEntity<Map<String, Object>> getCommissionRate(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "STARTER") String tier,
            @RequestParam(required = false) String bookingType) {
        // Ensure caller is authenticated (auth is non-null due to security config)
        UUID userId = UUID.fromString(auth.getName());
        BigDecimal rate = commissionService.getCommissionRate(tier, bookingType);
        return ResponseEntity.ok(Map.of(
                "tier", tier,
                "commissionRate", rate,
                "commissionPercent", rate.multiply(BigDecimal.valueOf(100))
        ));
    }
}
