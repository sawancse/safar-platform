package com.safar.insurance.adapter;

import com.safar.insurance.entity.enums.CoverageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Fully-mocked insurance provider — quote/issue/cancel work end-to-end with NO
 * partner credentials. Default provider until a real aggregator/insurer is wired.
 * Premium + sum-insured are encoded into the quote token so {@link #issue} can echo
 * the exact figures back (the token is opaque to the rest of the system).
 * Covers every CoverageType (travel, stay/tenant embedded, and standalone health/
 * life/motor/home/PA marketplace products).
 */
@Component
@Slf4j
public class SandboxInsuranceProvider implements InsuranceProviderAdapter {

    @Value("${insurance.sandbox.enabled:true}")
    private boolean enabled;

    @Override public InsuranceProvider providerType() { return InsuranceProvider.SANDBOX; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public QuoteResult quote(QuoteRequest req) {
        long days = (req.tripStartDate() != null && req.tripEndDate() != null)
                ? Math.max(1, ChronoUnit.DAYS.between(req.tripStartDate(), req.tripEndDate())) : 0;
        int travellers = req.travellerAges() != null && !req.travellerAges().isEmpty() ? req.travellerAges().size() : 1;
        CoverageType c = req.coverageType();

        long premium;
        long sumInsured;
        List<String> highlights;
        switch (c) {
            case INTERNATIONAL_TRAVEL, STUDENT_TRAVEL -> {
                premium = 49900 + 8000 * days + 29900L * (travellers - 1);
                sumInsured = 4150000000L;   // ~$50k
                highlights = List.of("Emergency medical up to $50,000", "Trip cancellation & delay", "Lost baggage & passport", "24x7 global assistance");
            }
            case DOMESTIC_TRAVEL -> {
                premium = 19900 + 5000 * days + 9900L * (travellers - 1);
                sumInsured = 50000000L;     // ₹5L
                highlights = List.of("Trip cancellation up to ₹50,000", "Baggage loss up to ₹25,000", "Personal accident cover", "Trip delay payout");
            }
            case STAY_PROTECTION -> {
                premium = 14900;
                sumInsured = 5000000L;
                highlights = List.of("Booking cancellation refund", "Stay interruption cover", "Emergency assistance");
            }
            case TENANT_CONTENTS -> {
                premium = 49900;
                sumInsured = 30000000L;
                highlights = List.of("Home contents up to ₹3L", "Personal liability", "Fire, theft & burglary");
            }
            case HEALTH -> {
                premium = 800000;           // ₹8,000/yr
                sumInsured = 50000000L;     // ₹5L
                highlights = List.of("₹5L hospitalisation cover", "Cashless network hospitals", "Pre/post hospitalisation", "Day-care procedures");
            }
            case LIFE_TERM -> {
                premium = 1200000;          // ₹12,000/yr
                sumInsured = 10000000000L;  // ₹1Cr
                highlights = List.of("₹1 Crore term cover", "Tax benefits u/s 80C", "Optional critical-illness rider");
            }
            case MOTOR -> {
                premium = 600000;
                sumInsured = 50000000L;
                highlights = List.of("Own-damage + third-party", "Zero-depreciation add-on", "Cashless garages", "Roadside assistance");
            }
            case HOME -> {
                premium = 300000;
                sumInsured = 500000000L;    // ₹50L
                highlights = List.of("Structure + contents", "Natural calamity cover", "Burglary & theft");
            }
            case PERSONAL_ACCIDENT -> {
                premium = 49900;
                sumInsured = 100000000L;    // ₹10L
                highlights = List.of("Accidental death & disability", "Hospital cash", "Worldwide cover");
            }
            default -> {
                premium = 19900;
                sumInsured = 10000000L;
                highlights = List.of("Standard cover");
            }
        }
        // token = premium_suminsured_rand so issue() can echo exact figures
        String token = premium + "_" + sumInsured + "_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SANDBOX insurance] quote {} -> premium ₹{}, cover ₹{}", c, premium / 100, sumInsured / 100);
        return new QuoteResult(InsuranceProvider.SANDBOX, token, premium, sumInsured, "INR", highlights,
                "https://sandbox.bhramankaro.com/insurance/policy-wording.pdf");
    }

    @Override
    public IssueResult issue(IssueRequest req) {
        long premium = 0, sumInsured = 0;
        try {
            String[] parts = req.providerQuoteToken().split("_");
            premium = Long.parseLong(parts[0]);
            sumInsured = Long.parseLong(parts[1]);
        } catch (Exception ignored) { /* token from a non-sandbox source */ }
        String policyId = "SBX-POL-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        log.info("[SANDBOX insurance] issued {} premium ₹{}", policyId, premium / 100);
        return new IssueResult(policyId,
                "https://sandbox.bhramankaro.com/insurance/certificate/" + policyId + ".pdf",
                premium, sumInsured, "INR", "ISSUED");
    }

    @Override
    public void cancel(String externalPolicyId) {
        log.info("[SANDBOX insurance] cancelled policy {}", externalPolicyId);
    }
}
