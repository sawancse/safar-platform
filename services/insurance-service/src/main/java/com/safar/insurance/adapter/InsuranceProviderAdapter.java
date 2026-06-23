package com.safar.insurance.adapter;

/**
 * Pluggable insurance provider. Day-1 implementations: Acko, ICICI Lombard
 * (both stubs awaiting partnership creds). Adapter pattern mirrors the
 * flight-service FlightProviderAdapter — same registration + isEnabled +
 * canBook rhythm.
 */
public interface InsuranceProviderAdapter {

    InsuranceProvider providerType();

    /** Whether creds + enabled flag are set. False ⇒ excluded from quote fan-out. */
    boolean isEnabled();

    /** True if the adapter can issue policies (vs being quote-only). */
    default boolean canIssue() { return true; }

    /**
     * Get a quote from this provider. Should be fast (&lt;3s); the QuoteService
     * fans out across all enabled adapters in parallel.
     */
    QuoteResult quote(QuoteRequest request);

    /**
     * PolicyBazaar-style multi-plan compare: return ALL plans this provider offers for the
     * request (across insurers / tiers), each with a bindable quoteId. Default wraps the
     * single best quote() into one option so quote-only stubs still participate.
     */
    default java.util.List<com.safar.insurance.dto.PlanOption> comparePlans(QuoteRequest request) {
        QuoteResult q = quote(request);
        return java.util.List.of(new com.safar.insurance.dto.PlanOption(
                ProviderQuoteId.encode(q.provider(), q.providerQuoteToken()),
                q.provider().name(), "Standard Plan", null,
                q.premiumPaise(), q.sumInsuredPaise(), q.currency(),
                q.coverageHighlights(), null, null, null,
                java.util.List.of(), q.fareRulesUrl(), false, false));
    }

    /**
     * Issue a policy. Provider creates the cert, returns external ID + URL.
     * @throws InsuranceProviderException on failure (caller decides how to surface)
     */
    IssueResult issue(IssueRequest request);

    /**
     * Cancel an existing policy. Provider determines refund eligibility per
     * the policy's cancellation grid (typically 100% refund within 14-day
     * "free look" period; partial after; nil after trip start).
     * No-op if externalPolicyId is null/blank.
     */
    void cancel(String externalPolicyId);
}
