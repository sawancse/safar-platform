package com.safar.insurance.adapter;

import java.util.List;

/**
 * One provider's quote response. The caller (UI) sees a list of these
 * across all enabled providers, sorted by premium.
 *
 * The {@code providerQuoteToken} is opaque — the provider may need it
 * back at issue() time to honour the same premium (locks in the rate
 * for ~15 min typically).
 */
public record QuoteResult(
        InsuranceProvider provider,
        String providerQuoteToken,
        long premiumPaise,
        long sumInsuredPaise,
        String currency,
        List<String> coverageHighlights,    // e.g. ["Trip cancellation up to ₹50k", "Lost baggage up to ₹25k"]
        String fareRulesUrl                  // link to the policy wording PDF
) {}
