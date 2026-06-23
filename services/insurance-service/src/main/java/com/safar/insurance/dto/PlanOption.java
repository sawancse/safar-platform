package com.safar.insurance.dto;

import java.util.List;

/**
 * One comparable insurance plan in a PolicyBazaar-style compare grid. A single
 * coverage type fans out into many of these (across insurers / tiers). The
 * {@code quoteId} is the bindable, provider-prefixed token (PROVIDER:nativeToken)
 * the customer carries into /buy so issue() honours the exact plan + premium.
 */
public record PlanOption(
        String quoteId,                 // PROVIDER:nativeToken — pass straight to /buy
        String insurer,                 // underwriting insurer name
        String planName,                // marketed plan name (e.g. "Optima Secure")
        String tagline,
        long premiumPaise,              // base annual/trip premium (before add-ons)
        long sumInsuredPaise,           // cover amount
        String currency,
        List<String> features,          // headline inclusions
        Double claimSettlementRatio,    // % e.g. 98.5 — null if unknown
        Integer cashlessCount,          // cashless hospitals (health) / garages (motor) / null
        Double insurerRating,           // 0–5 customer rating — null if unknown
        List<AddOn> addOns,             // selectable riders that bump premium
        String wordingUrl,              // policy wording PDF
        boolean recommended,            // surface a "Recommended" badge
        boolean sandbox                 // true = demo data (no real underwriter yet)
) {
    public record AddOn(String code, String label, long premiumPaise) {}
}
