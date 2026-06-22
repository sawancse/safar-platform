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
    private final com.safar.insurance.repository.InsurancePolicyRepository policyRepository;

    public record ProductCard(String key, String category, String coverageType, String title,
                              String tagline, List<String> highlights, String applyPath) {}
    public record MarketplaceQuoteRequest(CoverageType coverageType, Integer tenureDays, Integer ageYears) {}
    public record MarketplaceQuoteResponse(String quoteId, long premiumPaise, long sumInsuredPaise,
                                           String currency, List<String> coverageHighlights) {}
    public record MarketplaceBuyRequest(String quoteId, CoverageType coverageType, String fullName,
                                        String contactEmail, String contactPhone, String bookingId) {}

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

    @PostMapping("/buy")
    public ResponseEntity<Map<String, Object>> buy(Authentication auth, @RequestBody MarketplaceBuyRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        String quoteId = req.quoteId();
        if (quoteId == null || quoteId.isBlank()) {
            QuoteResult q = registry.primary().quote(new QuoteRequest(
                    null, null, "IN", "IN", LocalDate.now().plusDays(1), LocalDate.now().plusDays(366), req.coverageType(), List.of(30)));
            quoteId = ProviderQuoteId.encode(q.provider(), q.providerQuoteToken());
        }
        String[] name = (req.fullName() != null ? req.fullName() : "Customer").trim().split(" ", 2);
        var traveller = new IssuePolicyRequest.TravellerDTO(
                name[0], name.length > 1 ? name[1] : name[0], LocalDate.now().minusYears(30), "X", "IN", null, null);
        IssuePolicyRequest issueReq = new IssuePolicyRequest(quoteId, null, null, "IN", "IN",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(366), req.coverageType(),
                List.of(traveller), req.contactEmail(), req.contactPhone());
        InsurancePolicy policy = policyService.issue(userId, issueReq);
        // Sandbox auto-confirm so it shows as ISSUED in My Policies.
        policy = policyService.confirmPayment(userId, policy.getId(), "SBX-ORDER-" + policy.getId(), "SBX-PAY");
        // Embedded (booking-attached) policy → link to the booking.
        if (req.bookingId() != null && !req.bookingId().isBlank()) {
            try {
                policy.setBookingId(UUID.fromString(req.bookingId()));
                policy = policyRepository.save(policy);
            } catch (IllegalArgumentException ignored) { /* malformed bookingId — leave standalone */ }
        }
        return ResponseEntity.ok(Map.of(
                "policyRef", policy.getPolicyRef(),
                "status", policy.getStatus(),
                "premiumPaise", policy.getPremiumPaise() != null ? policy.getPremiumPaise() : 0,
                "certificateUrl", policy.getCertificateUrl() != null ? policy.getCertificateUrl() : ""));
    }
}
