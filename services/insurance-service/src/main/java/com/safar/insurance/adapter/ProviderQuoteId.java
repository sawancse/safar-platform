package com.safar.insurance.adapter;

/**
 * Encodes the provider into the quoteId so issue() can route to the
 * right adapter — "ACKO:nativeToken" / "ICICI_LOMBARD:nativeToken".
 * Same pattern as flight-service ProviderOfferId.
 */
public final class ProviderQuoteId {

    private static final String SEP = ":";

    private ProviderQuoteId() {}

    public static String encode(InsuranceProvider provider, String nativeToken) {
        return provider.name() + SEP + nativeToken;
    }

    public static InsuranceProvider provider(String prefixed) {
        int i = sepIndex(prefixed);
        try {
            return InsuranceProvider.valueOf(prefixed.substring(0, i));
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed provider quote id: " + prefixed);
        }
    }

    public static String nativeToken(String prefixed) {
        return prefixed.substring(sepIndex(prefixed) + 1);
    }

    private static int sepIndex(String prefixed) {
        if (prefixed == null) throw new IllegalArgumentException("quoteId is null");
        int i = prefixed.indexOf(SEP);
        if (i <= 0 || i == prefixed.length() - 1) {
            throw new IllegalArgumentException("Malformed provider quote id (expected PROVIDER:token): " + prefixed);
        }
        return i;
    }
}
