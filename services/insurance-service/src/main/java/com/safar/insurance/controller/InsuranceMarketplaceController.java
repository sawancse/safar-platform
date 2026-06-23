package com.safar.insurance.controller;

import com.safar.insurance.adapter.*;
import com.safar.insurance.dto.IssuePolicyRequest;
import com.safar.insurance.entity.InsurancePolicy;
import com.safar.insurance.entity.enums.CoverageType;
import com.safar.insurance.service.InsurancePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standalone "Insurance & Loans" marketplace — sell insurance (and surface loans)
 * with NO booking attached. Catalog + quote are public; buy issues a real policy
 * (Sandbox provider by default) reusing the core issue/confirm flow.
 */
@RestController
@RequestMapping("/api/v1/insurance/marketplace")
@RequiredArgsConstructor
public class InsuranceMarketplaceController {

    private final InsuranceProviderRegistry registry;
    private final InsurancePolicyService policyService;
    private final com.safar.insurance.service.InsuranceLeadService leadService;
    private final com.safar.insurance.service.RazorpayService razorpayService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final com.safar.insurance.repository.InsurancePolicyRepository policyRepository;

    @org.springframework.beans.factory.annotation.Value("${insurance.internal.token:safar-internal-insurance-2026}")
    private String internalToken;

    public record ProductCard(String key, String category, String coverageType, String title,
                              String tagline, List<String> highlights, String applyPath) {}
    public record MarketplaceQuoteRequest(CoverageType coverageType, Integer tenureDays, Integer ageYears) {}
    public record MarketplaceQuoteResponse(String quoteId, long premiumPaise, long sumInsuredPaise,
                                           String currency, List<String> coverageHighlights) {}
    public record MarketplaceBuyRequest(String quoteId, CoverageType coverageType, String fullName,
                                        String contactEmail, String contactPhone, String bookingId,
                                        List<String> addOnCodes) {}

    /** PolicyBazaar-style compare request. ages = per-member ages (health/travel); tenureDays for travel. */
    public record CompareRequest(CoverageType coverageType, Integer tenureDays, List<Integer> ages,
                                 String originCode, String destinationCode) {}
    public record CompareResponse(java.util.List<com.safar.insurance.dto.PlanOption> plans) {}

    public record AdvisorCallbackRequest(String name, String phone, String email, String product,
                                         CoverageType coverageType, String city, String preferredTime, String notes) {}

    /** Catalog for the hub: insurance products (buyable here) + loans (link to existing VAS). */
    @GetMapping("/products")
    public List<ProductCard> products() {
        return List.of(
            new ProductCard("health", "INSURANCE", "HEALTH", "Health Insurance", "Cashless hospitalisation up to ₹5L",
                    List.of("₹5L cover", "Cashless network", "Tax benefits"), null),
            new ProductCard("term-life", "INSURANCE", "LIFE_TERM", "Term Life", "₹1 Crore cover for your family",
                    List.of("₹1Cr cover", "80C tax benefit", "Riders"), null),
            new ProductCard("motor", "INSURANCE", "MOTOR", "Car & Bike Insurance", "Own-damage + third-party",
                    List.of("Zero-dep add-on", "Cashless garages", "Roadside assist"), null),
            new ProductCard("travel", "INSURANCE", "INTERNATIONAL_TRAVEL", "Travel Insurance", "Cover for your next trip",
                    List.of("Medical $50k", "Trip cancellation", "Baggage"), null),
            new ProductCard("home", "INSURANCE", "HOME", "Home Insurance", "Structure + contents protection",
                    List.of("₹50L cover", "Natural calamity", "Burglary"), null),
            new ProductCard("personal-accident", "INSURANCE", "PERSONAL_ACCIDENT", "Personal Accident", "₹10L accident cover",
                    List.of("Death & disability", "Hospital cash", "Worldwide"), null),
            new ProductCard("tenant", "INSURANCE", "TENANT_CONTENTS", "Renter / Tenant Cover", "Protect your rented home",
                    List.of("Contents ₹3L", "Liability", "Fire & theft"), null),
            // Loans — handled by the existing Home-Loan VAS + generic enquiry
            new ProductCard("home-loan", "LOAN", null, "Home Loan", "15 partner banks, lowest EMIs",
                    List.of("Up to ₹5Cr", "EMI calculator", "Quick eligibility"), "/services/homeloan"),
            new ProductCard("personal-loan", "LOAN", null, "Personal Loan", "Instant personal loans",
                    List.of("Up to ₹40L", "Minimal docs", "Fast disbursal"), "/services/loans/personal"),
            new ProductCard("business-loan", "LOAN", null, "Business Loan", "Working capital & term loans",
                    List.of("Up to ₹2Cr", "Collateral-free options"), "/services/loans/business"),
            new ProductCard("lap", "LOAN", null, "Loan Against Property", "Unlock your property's value",
                    List.of("Up to ₹5Cr", "Low interest"), "/services/loans/lap")
        );
    }

    @PostMapping("/quote")
    public ResponseEntity<MarketplaceQuoteResponse> quote(@RequestBody MarketplaceQuoteRequest req) {
        int tenure = req.tenureDays() != null ? req.tenureDays() : 365;
        int age = req.ageYears() != null ? req.ageYears() : 30;
        LocalDate start = LocalDate.now().plusDays(1);
        QuoteResult q = registry.primary().quote(new QuoteRequest(
                null, null, "IN", "IN", start, start.plusDays(tenure), req.coverageType(), List.of(age)));
        return ResponseEntity.ok(new MarketplaceQuoteResponse(
                ProviderQuoteId.encode(q.provider(), q.providerQuoteToken()),
                q.premiumPaise(), q.sumInsuredPaise(), q.currency(), q.coverageHighlights()));
    }

    /** PolicyBazaar-style compare — all plans across insurers for the chosen product. Public. */
    @PostMapping("/compare")
    public ResponseEntity<CompareResponse> compare(@RequestBody CompareRequest req) {
        int tenure = req.tenureDays() != null ? req.tenureDays() : 365;
        List<Integer> ages = (req.ages() != null && !req.ages().isEmpty()) ? req.ages() : List.of(30);
        LocalDate start = LocalDate.now().plusDays(1);
        QuoteRequest qr = new QuoteRequest(
                req.originCode(), req.destinationCode(), "IN", "IN",
                start, start.plusDays(tenure), req.coverageType(), ages);
        var plans = registry.primary().comparePlans(qr);
        return ResponseEntity.ok(new CompareResponse(plans));
    }

    /** Talk-to-advisor / assisted sales lead. Public (anonymous allowed). */
    @PostMapping("/advisor-callback")
    public ResponseEntity<Map<String, Object>> advisorCallback(Authentication auth,
                                                               @RequestBody AdvisorCallbackRequest req) {
        if (req.phone() == null || req.phone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone required"));
        }
        UUID userId = (auth != null && auth.getName() != null) ? tryUuid(auth.getName()) : null;
        var lead = leadService.create(userId, req.name(), req.phone(), req.email(),
                req.product(), req.coverageType() != null ? req.coverageType().name() : null,
                req.city(), req.preferredTime(), req.notes());
        return ResponseEntity.ok(Map.of("leadId", lead.getId().toString(), "status", lead.getStatus()));
    }

    private static UUID tryUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    /** Create a PENDING_PAYMENT policy (quote-if-needed → provider issue → link booking). */
    private InsurancePolicy createPending(UUID userId, CoverageType coverageType, String quoteId,
                                          String fullName, String contactEmail, String contactPhone, String bookingId) {
        if (quoteId == null || quoteId.isBlank()) {
            QuoteResult q = registry.primary().quote(new QuoteRequest(
                    null, null, "IN", "IN", LocalDate.now().plusDays(1), LocalDate.now().plusDays(366), coverageType, List.of(30)));
            quoteId = ProviderQuoteId.encode(q.provider(), q.providerQuoteToken());
        }
        String[] name = (fullName != null && !fullName.isBlank() ? fullName : "Customer").trim().split(" ", 2);
        var traveller = new IssuePolicyRequest.TravellerDTO(
                name[0], name.length > 1 ? name[1] : name[0], LocalDate.now().minusYears(30), "X", "IN", null, null);
        IssuePolicyRequest issueReq = new IssuePolicyRequest(quoteId, null, null, "IN", "IN",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(366), coverageType,
                List.of(traveller), contactEmail, contactPhone);
        InsurancePolicy policy = policyService.issue(userId, issueReq);
        if (bookingId != null && !bookingId.isBlank()) {
            try { policy.setBookingId(UUID.fromString(bookingId)); policy = policyRepository.save(policy); }
            catch (IllegalArgumentException ignored) { /* malformed bookingId — leave standalone */ }
        }
        return policy;
    }

    /** Shared instant-issue path (sandbox / embedded): create pending → sandbox-confirm. */
    private InsurancePolicy doIssue(UUID userId, CoverageType coverageType, String quoteId,
                                    String fullName, String contactEmail, String contactPhone, String bookingId) {
        InsurancePolicy policy = createPending(userId, coverageType, quoteId, fullName, contactEmail, contactPhone, bookingId);
        return policyService.confirmPayment(userId, policy.getId(), "SBX-ORDER-" + policy.getId(), "SBX-PAY");
    }

    private Map<String, Object> policyResponse(InsurancePolicy p) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("policyRef", p.getPolicyRef());
        m.put("status", p.getStatus());
        m.put("premiumPaise", p.getPremiumPaise() != null ? p.getPremiumPaise() : 0);
        m.put("certificateUrl", p.getCertificateUrl() != null ? p.getCertificateUrl() : "");
        return m;
    }

    @PostMapping("/buy")
    public ResponseEntity<Map<String, Object>> buy(Authentication auth, @RequestBody MarketplaceBuyRequest req) {
        String quoteId = applyAddOns(req.quoteId(), req.coverageType(), req.addOnCodes());
        InsurancePolicy policy = doIssue(UUID.fromString(auth.getName()), req.coverageType(), quoteId,
                req.fullName(), req.contactEmail(), req.contactPhone(), req.bookingId());
        return ResponseEntity.ok(policyResponse(policy));
    }

    // ── Two-step paid flow (real Razorpay when enabled; sandbox when not) ──
    public record ConfirmPaymentRequest(String policyId, String razorpayOrderId,
                                        String razorpayPaymentId, String razorpaySignature) {}

    /**
     * Step 1: create a PENDING_PAYMENT policy and (if Razorpay is live) a Razorpay order.
     * When Razorpay is disabled (sandbox), returns razorpayEnabled=false and the client
     * proceeds straight to /confirm-payment (no charge).
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(Authentication auth, @RequestBody MarketplaceBuyRequest req) {
        String quoteId = applyAddOns(req.quoteId(), req.coverageType(), req.addOnCodes());
        InsurancePolicy policy = createPending(UUID.fromString(auth.getName()), req.coverageType(), quoteId,
                req.fullName(), req.contactEmail(), req.contactPhone(), req.bookingId());
        long premium = policy.getPremiumPaise() != null ? policy.getPremiumPaise() : 0;
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("policyId", policy.getId().toString());
        resp.put("policyRef", policy.getPolicyRef());
        resp.put("premiumPaise", premium);
        if (razorpayService.isEnabled() && premium > 0) {
            String orderId = razorpayService.createOrder(premium, policy.getPolicyRef());
            resp.put("razorpayEnabled", true);
            resp.put("razorpayKeyId", razorpayService.getKeyId());
            resp.put("razorpayOrderId", orderId);
            resp.put("amountPaise", premium);
        } else {
            resp.put("razorpayEnabled", false);
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * Step 2: verify the Razorpay signature (when live) and finalise the policy (ISSUED + cert email).
     * In sandbox mode the signature is skipped and the policy is confirmed directly.
     */
    @PostMapping("/confirm-payment")
    public ResponseEntity<Map<String, Object>> confirmPayment(Authentication auth, @RequestBody ConfirmPaymentRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        UUID policyId = UUID.fromString(req.policyId());
        String orderId, paymentId;
        if (razorpayService.isEnabled()) {
            if (!razorpayService.verifyPaymentSignature(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature())) {
                return ResponseEntity.status(400).body(Map.of("error", "payment signature verification failed"));
            }
            orderId = req.razorpayOrderId();
            paymentId = req.razorpayPaymentId();
        } else {
            orderId = "SBX-ORDER-" + policyId;
            paymentId = "SBX-PAY";
        }
        InsurancePolicy policy = policyService.confirmPayment(userId, policyId, orderId, paymentId);
        return ResponseEntity.ok(policyResponse(policy));
    }

    /**
     * Razorpay webhook — HMAC-verified. Handles refunds (refund.processed / payment.refunded)
     * to mark the linked policy REFUNDED. Public path; the signature IS the auth.
     */
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Map<String, Object>> razorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawPayload) {
        if (!razorpayService.verifyWebhookSignature(rawPayload, signature)) {
            return ResponseEntity.status(403).body(Map.of("error", "invalid signature"));
        }
        try {
            var root = objectMapper.readTree(rawPayload);
            String event = root.path("event").asText("");
            if (event.startsWith("refund.") || event.equals("payment.refunded")) {
                var entity = root.path("payload").path("refund").path("entity");
                String paymentId = entity.path("payment_id").asText("");
                long refundPaise = entity.path("amount").asLong(0);
                policyRepository.findByRazorpayPaymentId(paymentId)
                        .ifPresent(p -> policyService.markRefunded(p, refundPaise));
            }
        } catch (Exception ignored) { /* ack anyway — Razorpay retries on non-2xx */ }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Fold selected add-on premiums into the quote. Sandbox tokens encode premium_suminsured_rand,
     * so we re-encode with the add-on-inclusive premium. For a live aggregator the add-on codes are
     * priced by the partner API at issue time (passed through; left unchanged here).
     */
    private String applyAddOns(String quoteId, CoverageType coverageType, List<String> addOnCodes) {
        if (quoteId == null || coverageType == null || addOnCodes == null || addOnCodes.isEmpty()) return quoteId;
        long addPaise = InsuranceAddOnCatalog.priceOf(coverageType, addOnCodes);
        if (addPaise <= 0) return quoteId;
        try {
            if (ProviderQuoteId.provider(quoteId) != InsuranceProvider.SANDBOX) return quoteId;
            String[] p = ProviderQuoteId.nativeToken(quoteId).split("_");
            long premium = Long.parseLong(p[0]) + addPaise;
            String rebuilt = premium + "_" + (p.length > 1 ? p[1] : "0") + "_" + (p.length > 2 ? p[2] : "addon");
            return ProviderQuoteId.encode(InsuranceProvider.SANDBOX, rebuilt);
        } catch (Exception e) {
            return quoteId;
        }
    }

    // ── Server-to-server issuance (booking-service on payment.captured) ──
    // Secret-gated (X-Internal-Token) + idempotent per booking. Not for browser use.
    public record InternalIssueRequest(String userId, String bookingId, CoverageType coverageType,
                                       String quoteId, String fullName, String contactEmail, String contactPhone) {}

    @PostMapping("/internal/issue")
    public ResponseEntity<Map<String, Object>> internalIssue(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody InternalIssueRequest req) {
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(token)) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }
        if (req.bookingId() != null && !req.bookingId().isBlank()) {
            try {
                var existing = policyRepository.findFirstByBookingId(UUID.fromString(req.bookingId()));
                if (existing.isPresent()) {
                    Map<String, Object> r = policyResponse(existing.get());
                    r.put("idempotent", true);
                    return ResponseEntity.ok(r);
                }
            } catch (IllegalArgumentException ignored) { /* fall through to issue */ }
        }
        InsurancePolicy policy = doIssue(UUID.fromString(req.userId()), req.coverageType(), req.quoteId(),
                req.fullName(), req.contactEmail(), req.contactPhone(), req.bookingId());
        return ResponseEntity.ok(policyResponse(policy));
    }
}
