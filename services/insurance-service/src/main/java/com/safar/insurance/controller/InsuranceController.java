package com.safar.insurance.controller;

import com.safar.insurance.dto.InsuranceQuoteRequest;
import com.safar.insurance.dto.InsuranceQuoteResponse;
import com.safar.insurance.dto.IssuePolicyRequest;
import com.safar.insurance.entity.InsurancePolicy;
import com.safar.insurance.service.InsurancePolicyService;
import com.safar.insurance.service.InsuranceQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
@Slf4j
public class InsuranceController {

    private final InsuranceQuoteService quoteService;
    private final InsurancePolicyService policyService;

    /** Public — no login required to see premiums. */
    @GetMapping("/quote")
    public ResponseEntity<InsuranceQuoteResponse> quote(@Valid InsuranceQuoteRequest request) {
        log.info("Insurance quote: {} → {} on {} (pax={})",
                request.tripOriginCode(), request.tripDestinationCode(),
                request.tripStartDate(), request.travellerAges().size());
        return ResponseEntity.ok(quoteService.quote(request));
    }

    @PostMapping("/issue")
    public ResponseEntity<InsurancePolicy> issue(
            Authentication auth,
            @Valid @RequestBody IssuePolicyRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.issue(userId, request));
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<InsurancePolicy> confirmPayment(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody Map<String, String> paymentDetails) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(policyService.confirmPayment(
                userId, id,
                paymentDetails.get("razorpayOrderId"),
                paymentDetails.get("razorpayPaymentId")
        ));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InsurancePolicy> cancel(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(policyService.cancel(userId, id, reason));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<InsurancePolicy>> myPolicies(
            Authentication auth,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(policyService.getMyPolicies(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsurancePolicy> get(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.getPolicy(id));
    }
}
