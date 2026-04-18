package com.safar.payment.controller;

import com.safar.payment.entity.TenancySubscription;
import com.safar.payment.service.TenancyPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/tenancy")
@RequiredArgsConstructor
public class TenancyPaymentController {

    private final TenancyPaymentService tenancyPaymentService;

    @PostMapping("/{tenancyId}/subscription")
    public ResponseEntity<Map<String, Object>> createSubscription(
            @PathVariable UUID tenancyId,
            @RequestBody Map<String, Object> body) {
        UUID tenantId = UUID.fromString((String) body.get("tenantId"));
        long amountPaise = ((Number) body.get("amountPaise")).longValue();
        String tenancyRef = (String) body.getOrDefault("tenancyRef", "PGT-UNKNOWN");

        TenancyPaymentService.CreateSubResult result = tenancyPaymentService.createRentSubscriptionWithUrl(
                tenancyId, tenantId, amountPaise, tenancyRef);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", result.subscription().getId());
        resp.put("tenancyId", result.subscription().getTenancyId());
        resp.put("razorpaySubscriptionId", result.subscription().getRazorpaySubscriptionId());
        resp.put("razorpayPlanId", result.subscription().getRazorpayPlanId());
        resp.put("amountPaise", result.subscription().getAmountPaise());
        resp.put("status", result.subscription().getStatus());
        resp.put("shortUrl", result.shortUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{tenancyId}/subscription")
    public ResponseEntity<TenancySubscription> getSubscription(@PathVariable UUID tenancyId) {
        return ResponseEntity.ok(tenancyPaymentService.getByTenancyId(tenancyId));
    }

    @PostMapping("/{tenancyId}/subscription/cancel")
    public ResponseEntity<Void> cancelSubscription(@PathVariable UUID tenancyId) {
        tenancyPaymentService.cancelSubscription(tenancyId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/subscription")
    public ResponseEntity<Void> subscriptionWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        tenancyPaymentService.handleSubscriptionWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
