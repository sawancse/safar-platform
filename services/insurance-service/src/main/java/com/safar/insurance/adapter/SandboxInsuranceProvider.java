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

    @Value("${insurance.public-base-url:https://api.bhramankaro.com}")
    private String publicBaseUrl;

    @Override public InsuranceProvider providerType() { return InsuranceProvider.SANDBOX; }
    @Override public boolean isEnabled() { return enabled; }

    /** Self-hosted policy-wording doc (the old sandbox.bhramankaro.com host didn't exist). */
    private String wordingUrl(CoverageType c) {
        return publicBaseUrl + "/api/v1/insurance/policy-wording?type=" + c.name();
    }

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
                wordingUrl(req.coverageType()));
    }

    // ── PolicyBazaar-style compare: many plans across insurers/tiers ────────────
    @Override
    public List<PlanOption> comparePlans(QuoteRequest req) {
        Base b = base(req);
        List<PlanOption> plans = new ArrayList<>();
        for (PlanSpec s : specs(req.coverageType(), b)) {
            long premium = Math.max(1, Math.round(b.premium * s.premiumMult()));   // scales with age/members/tenure
            long cover = s.coverPaise() > 0 ? s.coverPaise() : b.sumInsured;
            List<String> features = (s.features() != null && !s.features().isEmpty()) ? s.features() : b.highlights;
            String token = premium + "_" + cover + "_" + UUID.randomUUID().toString().substring(0, 8);
            plans.add(new PlanOption(
                    ProviderQuoteId.encode(InsuranceProvider.SANDBOX, token),
                    s.insurer(), s.planName(), s.tagline(),
                    premium, cover, "INR", features,
                    s.claimRatio(), s.cashless(), s.rating(),
                    InsuranceAddOnCatalog.addOns(req.coverageType()),
                    wordingUrl(req.coverageType()),
                    s.recommended(), true));
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

    // ── demo plan catalog (real-world insurer products; sandbox=true on every plan) ──
    // Premiums scale off the per-product baseline; cover/claim-ratio/cashless are realistic
    // published-style figures. Replaced by the live aggregator's authoritative data at go-live.
    private record PlanSpec(String insurer, String planName, String tagline, double premiumMult,
                            long coverPaise, double claimRatio, Integer cashless, double rating,
                            boolean recommended, List<String> features) {}

    private static final long L = 10_000_000L;        // ₹1 lakh in paise
    private static final long CR = 1_000_000_000L;     // ₹1 crore in paise

    private List<PlanSpec> specs(CoverageType c, Base b) {
        return switch (c) {
            case HEALTH -> List.of(
                new PlanSpec("HDFC ERGO", "Optima Secure", "Cashless hospitalisation", 1.15, 10 * L, 99.0, 13000, 4.5, true,
                    List.of("₹10L cover", "2x cover in 2 years (Secure benefit)", "No room-rent capping", "13,000+ cashless hospitals", "Pre & post hospitalisation")),
                new PlanSpec("Star Health", "Star Comprehensive", "Widest hospital network", 1.00, 10 * L, 99.1, 14000, 4.4, false,
                    List.of("₹10L cover", "14,000+ cashless hospitals", "Maternity & newborn cover", "Automatic restoration of sum insured", "Annual health check-up")),
                new PlanSpec("Niva Bupa", "ReAssure 2.0", "Unlimited reinstatement", 1.35, 25 * L, 91.0, 10000, 4.3, false,
                    List.of("₹25L cover", "ReAssure forever benefit", "Lock the clock — entry-age pricing", "Booster+ carry forward", "10,000+ hospitals")),
                new PlanSpec("Care Health", "Care Supreme", "High no-claim bonus", 0.95, 10 * L, 90.0, 22000, 4.2, false,
                    List.of("₹10L cover", "Up to 100% no-claim bonus", "Unlimited recharge of cover", "22,000+ cashless hospitals", "Wellness benefits")),
                new PlanSpec("ICICI Lombard", "Elevate", "Powerful add-ons", 1.60, 50 * L, 97.0, 10000, 4.4, false,
                    List.of("₹50L cover", "Infinite sum-insured add-on", "Worldwide coverage", "Claim protector add-on", "10,000+ hospitals")));
            case LIFE_TERM -> List.of(
                new PlanSpec("HDFC Life", "Click 2 Protect Super", "Top claim settlement", 1.00, 1 * CR, 99.5, null, 4.5, true,
                    List.of("₹1Cr cover", "99.5% claims settled", "Life / Income / Both options", "In-built terminal illness", "Tax benefits 80C & 10(10D)")),
                new PlanSpec("ICICI Pru", "iProtect Smart", "Critical-illness option", 0.95, 1 * CR, 99.2, null, 4.4, false,
                    List.of("₹1Cr cover", "34 critical illnesses option", "Accidental death benefit", "Terminal illness cover")),
                new PlanSpec("Max Life", "Smart Secure Plus", "Return-of-premium option", 1.20, 150 * L, 99.5, null, 4.5, false,
                    List.of("₹1.5Cr cover", "Return of premium option", "Accelerated terminal illness", "Premium break benefit")),
                new PlanSpec("Tata AIA", "Sampoorna Raksha Promise", "Flexible payouts", 0.90, 1 * CR, 99.0, null, 4.3, false,
                    List.of("₹1Cr cover", "Multiple payout options", "Whole-life cover option", "In-built terminal illness")),
                new PlanSpec("Bajaj Allianz Life", "Smart Protect Goal", "Higher cover value", 1.40, 2 * CR, 99.0, null, 4.2, false,
                    List.of("₹2Cr cover", "Increasing cover option", "Child education protection", "Accidental death benefit")));
            case MOTOR -> List.of(
                new PlanSpec("Acko", "Comprehensive Car", "Fully digital, low cost", 0.85, 6 * L, 95.0, 1500, 4.3, true,
                    List.of("Own-damage + third-party", "Zero-commission pricing", "Doorstep pickup & repair", "Fast app-based claims")),
                new PlanSpec("HDFC ERGO", "Comprehensive Motor", "Largest garage network", 1.00, 6 * L, 99.0, 8000, 4.3, false,
                    List.of("Own-damage + third-party", "8,000+ cashless garages", "Overnight vehicle repair", "24x7 emergency assistance")),
                new PlanSpec("ICICI Lombard", "Comprehensive Car", "Video claim settlement", 1.05, 65 * L / 10, 98.0, 5600, 4.2, false,
                    List.of("Own-damage + third-party", "InstaSpect video claims", "5,600+ cashless garages", "Roadside assistance")),
                new PlanSpec("Bajaj Allianz", "Comprehensive", "On-the-spot claims", 0.95, 6 * L, 98.0, 6500, 4.1, false,
                    List.of("Own-damage + third-party", "6,500+ cashless garages", "Self-service claim app", "24x7 assistance")),
                new PlanSpec("Tata AIG", "Auto Secure", "Rich add-ons", 1.10, 6 * L, 98.0, 7500, 4.1, false,
                    List.of("Own-damage + third-party", "Zero-dep + engine secure add-ons", "7,500+ garages", "Auto SOS assistance")));
            case INTERNATIONAL_TRAVEL, DOMESTIC_TRAVEL, STUDENT_TRAVEL -> List.of(
                new PlanSpec("Tata AIG", "Travel Guard", "Global assistance", 1.00, b.sumInsured, 92.0, null, 4.3, true,
                    List.of("Emergency medical up to $50,000", "Trip cancellation & delay", "Baggage & passport loss", "24x7 global assistance")),
                new PlanSpec("Bajaj Allianz", "Travel Elite", "High medical limits", 1.10, b.sumInsured, 91.0, null, 4.2, false,
                    List.of("Emergency medical & evacuation", "Adventure sports option", "Trip interruption", "Cashless treatment abroad")),
                new PlanSpec("HDFC ERGO", "Travel Insurance", "Fast claims", 1.05, b.sumInsured, 93.0, null, 4.3, false,
                    List.of("Medical + evacuation cover", "Flight delay payout", "Loss of passport", "Personal liability")),
                new PlanSpec("Reliance General", "Travel Care", "Budget friendly", 0.90, b.sumInsured, 90.0, null, 4.0, false,
                    List.of("Emergency medical", "Baggage loss cover", "Trip cancellation", "Daily hospital cash")),
                new PlanSpec("Care Health", "Explore", "Family floater travel", 0.95, b.sumInsured, 90.0, null, 4.1, false,
                    List.of("Medical cover abroad", "Adventure sports add-on", "Home contents while away", "Automatic trip extension")));
            default -> List.of(
                new PlanSpec("HDFC ERGO", "Secure Plan", "Trusted cover", 1.00, b.sumInsured, 98.0, null, 4.3, true, b.highlights),
                new PlanSpec("ICICI Lombard", "Complete Plan", "Wider protection", 1.15, Math.round(b.sumInsured * 1.5), 97.0, null, 4.2, false, b.highlights),
                new PlanSpec("Bajaj Allianz", "Value Plan", "Budget friendly", 0.90, b.sumInsured, 96.0, null, 4.0, false, b.highlights));
        };
    }

}
