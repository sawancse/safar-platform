package com.safar.insurance.adapter;

import com.safar.insurance.dto.PlanOption;
import com.safar.insurance.entity.enums.CoverageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fully-mocked insurance provider — quote/compare/issue/cancel work end-to-end with NO
 * partner credentials. Default provider until the real aggregator is wired.
 *
 * comparePlans() generates a PolicyBazaar-style spread of plans across insurers/tiers so
 * the compare grid, add-ons and plan-detail are all populated for demos. Every plan carries
 * {@code sandbox=true}. Premium + sum-insured are encoded into the quote token so issue()
 * echoes the exact figures (the token is opaque to the rest of the system).
 */
@Component
@Slf4j
public class SandboxInsuranceProvider implements InsuranceProviderAdapter {

    @Value("${insurance.sandbox.enabled:true}")
    private boolean enabled;

    @Override public InsuranceProvider providerType() { return InsuranceProvider.SANDBOX; }
    @Override public boolean isEnabled() { return enabled; }

    private record Base(long premium, long sumInsured, List<String> highlights) {}

    /** Baseline premium + cover + highlights per coverage type (single-plan baseline). */
    private Base base(QuoteRequest req) {
        long days = (req.tripStartDate() != null && req.tripEndDate() != null)
                ? Math.max(1, ChronoUnit.DAYS.between(req.tripStartDate(), req.tripEndDate())) : 0;
        int travellers = req.travellerAges() != null && !req.travellerAges().isEmpty() ? req.travellerAges().size() : 1;
        switch (req.coverageType()) {
            case INTERNATIONAL_TRAVEL, STUDENT_TRAVEL -> {
                return new Base(49900 + 8000 * days + 29900L * (travellers - 1), 4150000000L,
                        List.of("Emergency medical up to $50,000", "Trip cancellation & delay", "Lost baggage & passport", "24x7 global assistance"));
            }
            case DOMESTIC_TRAVEL -> {
                return new Base(19900 + 5000 * days + 9900L * (travellers - 1), 50000000L,
                        List.of("Trip cancellation up to ₹50,000", "Baggage loss up to ₹25,000", "Personal accident cover", "Trip delay payout"));
            }
            case STAY_PROTECTION -> {
                return new Base(14900, 5000000L, List.of("Booking cancellation refund", "Stay interruption cover", "Emergency assistance"));
            }
            case TENANT_CONTENTS -> {
                return new Base(49900, 30000000L, List.of("Home contents up to ₹3L", "Personal liability", "Fire, theft & burglary"));
            }
            case HEALTH -> {
                return new Base(800000, 50000000L, List.of("Cashless hospitalisation", "Pre/post hospitalisation", "Day-care procedures", "No-claim bonus"));
            }
            case LIFE_TERM -> {
                return new Base(1200000, 10000000000L, List.of("High life cover", "Tax benefits u/s 80C", "Terminal illness benefit"));
            }
            case MOTOR -> {
                return new Base(600000, 50000000L, List.of("Own-damage + third-party", "Cashless garages", "Personal accident cover"));
            }
            case HOME -> {
                return new Base(300000, 500000000L, List.of("Structure + contents", "Natural calamity cover", "Burglary & theft"));
            }
            case PERSONAL_ACCIDENT -> {
                return new Base(49900, 100000000L, List.of("Accidental death & disability", "Hospital cash", "Worldwide cover"));
            }
            default -> {
                return new Base(19900, 10000000L, List.of("Standard cover"));
            }
        }
    }

    @Override
    public QuoteResult quote(QuoteRequest req) {
        Base b = base(req);
        String token = b.premium + "_" + b.sumInsured + "_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SANDBOX insurance] quote {} -> premium ₹{}, cover ₹{}", req.coverageType(), b.premium / 100, b.sumInsured / 100);
        return new QuoteResult(InsuranceProvider.SANDBOX, token, b.premium, b.sumInsured, "INR", b.highlights,
                "https://sandbox.bhramankaro.com/insurance/policy-wording.pdf");
    }

    // ── PolicyBazaar-style compare: many plans across insurers/tiers ────────────
    @Override
    public List<PlanOption> comparePlans(QuoteRequest req) {
        Base b = base(req);
        String[] insurers = insurers(req.coverageType());
        // tier: name, premiumMult, coverMult, claimRatio, rating, recommended
        Object[][] tiers = {
                {"Smart",   0.85, 0.8, 92.4, 4.0, false},
                {"Plus",    1.00, 1.0, 95.1, 4.3, true},
                {"Optima",  1.20, 1.5, 97.6, 4.5, false},
                {"Supreme", 1.45, 2.0, 98.9, 4.6, false},
                {"Elite",   1.75, 3.0, 99.2, 4.7, false},
        };
        List<PlanOption> plans = new ArrayList<>();
        for (int i = 0; i < tiers.length && i < insurers.length; i++) {
            Object[] t = tiers[i];
            String tier = (String) t[0];
            long premium = Math.round(b.premium * (double) t[1]);
            long cover = Math.round(b.sumInsured * (double) t[2]);
            double claim = (double) t[3];
            double rating = (double) t[4];
            boolean recommended = (boolean) t[5];
            String insurer = insurers[i];

            List<String> features = new ArrayList<>(b.highlights);
            if (cover > b.sumInsured) features.add(0, "Higher cover ₹" + (cover / 100));

            String token = premium + "_" + cover + "_" + UUID.randomUUID().toString().substring(0, 8);
            plans.add(new PlanOption(
                    ProviderQuoteId.encode(InsuranceProvider.SANDBOX, token),
                    insurer, insurer + " " + tier,
                    tagline(req.coverageType(), tier),
                    premium, cover, "INR", features,
                    claim, cashless(req.coverageType(), i), rating,
                    InsuranceAddOnCatalog.addOns(req.coverageType()),
                    "https://sandbox.bhramankaro.com/insurance/policy-wording.pdf",
                    recommended, true));
        }
        log.info("[SANDBOX insurance] compare {} -> {} plans", req.coverageType(), plans.size());
        return plans;
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

    // ── demo data helpers ───────────────────────────────────────────────────────

    private static String[] insurers(CoverageType c) {
        return switch (c) {
            case HEALTH -> new String[]{"Star Health", "HDFC ERGO", "Niva Bupa", "Care Health", "ICICI Lombard"};
            case LIFE_TERM -> new String[]{"HDFC Life", "ICICI Pru", "Max Life", "Tata AIA", "Bajaj Allianz Life"};
            case MOTOR -> new String[]{"Acko", "HDFC ERGO", "ICICI Lombard", "Bajaj Allianz", "Tata AIG"};
            case INTERNATIONAL_TRAVEL, DOMESTIC_TRAVEL, STUDENT_TRAVEL ->
                    new String[]{"Tata AIG", "Bajaj Allianz", "Reliance General", "HDFC ERGO", "Care Health"};
            default -> new String[]{"BhramanKaro Assured", "HDFC ERGO", "ICICI Lombard", "Bajaj Allianz", "Reliance General"};
        };
    }

    private static Integer cashless(CoverageType c, int idx) {
        return switch (c) {
            case HEALTH -> 7000 + idx * 1200;       // cashless hospitals
            case MOTOR -> 4000 + idx * 800;         // cashless garages
            default -> null;
        };
    }

    private static String tagline(CoverageType c, String tier) {
        return switch (c) {
            case HEALTH -> "Cashless hospitalisation • " + tier;
            case LIFE_TERM -> "Secure your family • " + tier;
            case MOTOR -> "Own-damage + third-party • " + tier;
            case INTERNATIONAL_TRAVEL, DOMESTIC_TRAVEL, STUDENT_TRAVEL -> "Travel worry-free • " + tier;
            default -> tier + " plan";
        };
    }

}
