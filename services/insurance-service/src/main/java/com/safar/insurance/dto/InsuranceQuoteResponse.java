package com.safar.insurance.dto;

import java.util.List;

public record InsuranceQuoteResponse(List<Quote> quotes) {

    /** Provider-prefixed quote token: "ACKO:abc" lets issue() route to the right adapter. */
    public record Quote(
            String quoteId,
            String provider,
            long premiumPaise,
            long sumInsuredPaise,
            String currency,
            List<String> coverageHighlights,
            String fareRulesUrl
    ) {}
}
